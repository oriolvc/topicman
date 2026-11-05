package com.oriolvc.topicman.messaging.application.usecase;

import static com.oriolvc.topicman.messaging.application.usecase.ConsumeSessionFixtures.NOW;
import static com.oriolvc.topicman.messaging.application.usecase.ConsumeSessionFixtures.aReceivedText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.messaging.application.model.CloseConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.model.ConsumeSessionSink;
import com.oriolvc.topicman.messaging.application.model.OpenConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.model.StreamConsumeSessionCommand;
import com.oriolvc.topicman.messaging.application.usecase.ConsumeSessionFixtures.CapturingExecutor;
import com.oriolvc.topicman.messaging.application.usecase.ConsumeSessionFixtures.ThreadPerTaskExecutor;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionInUseException;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.PayloadFormat;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class StreamConsumeSessionUseCaseTest {

    private static final Executor SAME_THREAD = Runnable::run;

    private final InMemoryBrokerEndpoints endpoints = new InMemoryBrokerEndpoints();
    private final FakeMessageReader kafka = new FakeMessageReader(BrokerType.KAFKA);
    private final ConsumeSessionRegistry registry = new ConsumeSessionRegistry();
    private final MutableClock clock = new MutableClock(NOW);
    private final OpenConsumeSessionUseCase open =
            new OpenConsumeSessionUseCase(endpoints, List.of(kafka), registry, ConsumeSessionFixtures.LIMITS, clock);
    private final CloseConsumeSessionUseCase close = new CloseConsumeSessionUseCase(registry, clock);
    private final ThreadPerTaskExecutor threads = new ThreadPerTaskExecutor();
    private Duration handoverTimeout = Duration.ofSeconds(5);

    @Test
    void deliversMessagesInOrderUntilTheClientLeaves() {
        var session = openSession();
        kafka.lastStream().willReturn(List.of(aReceivedText("first", "p@0"), aReceivedText("second", "p@1")));
        kafka.lastStream().willReturn(List.of(aReceivedText("third", "p@2")));
        var sink = new RecordingSink().leavingAfterMessages(3);

        stream(SAME_THREAD, session, sink);

        assertThat(sink.messages).extracting(ReceivedMessage::position).containsExactly("p@0", "p@1", "p@2");
    }

    @Test
    void keepsTheSessionOpenWhenTheClientLeaves() {
        var session = openSession();
        kafka.lastStream().willReturn(List.of(aReceivedText("first", "p@0")));

        stream(SAME_THREAD, session, new RecordingSink().leavingAfterMessages(1));

        assertThat(registry.contains(session)).isTrue();
        assertThat(kafka.lastStream().isClosed()).isFalse();
    }

    @Test
    void continuesWhereItStoppedWhenTheClientReconnects() {
        var session = openSession();
        kafka.lastStream().willReturn(List.of(aReceivedText("first", "p@0")));
        kafka.lastStream().willReturn(List.of(aReceivedText("second", "p@1")));
        stream(SAME_THREAD, session, new RecordingSink().leavingAfterMessages(1));
        var reconnected = new RecordingSink().leavingAfterMessages(1);

        stream(SAME_THREAD, session, reconnected);

        assertThat(reconnected.messages).extracting(ReceivedMessage::position).containsExactly("p@1");
    }

    @Test
    void keepsTheMessagesAClientLeftBeforeReceivingForTheNextClient() {
        var session = openSession();
        kafka.lastStream()
                .willReturn(List.of(
                        aReceivedText("first", "p@0"), aReceivedText("second", "p@1"), aReceivedText("third", "p@2")));
        stream(SAME_THREAD, session, new RecordingSink().leavingAfterMessages(1));
        var reconnected = new RecordingSink().leavingAfterMessages(2);

        stream(SAME_THREAD, session, reconnected);

        assertThat(reconnected.messages).extracting(ReceivedMessage::position).containsExactly("p@1", "p@2");
    }

    @Test
    void marksPayloadsThatParseAsJson() {
        var session = openSession();
        kafka.lastStream().willReturn(List.of(aReceivedText("{\"id\": 42}", "p@0"), aReceivedText("hello", "p@1")));
        var sink = new RecordingSink().leavingAfterMessages(2);

        stream(SAME_THREAD, session, sink);

        assertThat(sink.messages)
                .extracting(received -> received.message().payload().format())
                .containsExactly(PayloadFormat.JSON, PayloadFormat.TEXT);
        assertThat(sink.messages.getFirst().message().payload().content()).isEqualTo("{\"id\": 42}");
    }

    @Test
    void sendsAHeartbeatWhileNoMessageArrives() {
        var session = openSession();
        clock.advanceOnEveryRead(Duration.ofSeconds(10));
        var sink = new RecordingSink().leavingAfterHeartbeats(2);

        stream(SAME_THREAD, session, sink);

        assertThat(sink.heartbeats).isEqualTo(2);
        assertThat(sink.messages).isEmpty();
    }

    @Test
    void letsANewClientTakeOverTheStream() throws InterruptedException {
        var session = openSession();
        var first = new RecordingSink();
        stream(threads, session, first);

        stream(threads, session, new RecordingSink().leavingAfterMessages(0));
        threads.joinAll();

        assertThat(first.replaced).isTrue();
        assertThat(first.closed).isFalse();
        assertThat(registry.contains(session)).isTrue();
        assertThat(kafka.lastStream().isClosed()).isFalse();
    }

    @Test
    void handsTheMessagesNotYetDeliveredToTheClientTakingOver() throws InterruptedException {
        var session = openSession();
        kafka.lastStream()
                .willReturn(List.of(
                        aReceivedText("first", "p@0"), aReceivedText("second", "p@1"), aReceivedText("third", "p@2")));
        var second = new RecordingSink().leavingAfterMessages(2);
        var takeOver = new AtomicReference<TakeOver>();
        var first = new RecordingSink().afterFirstMessage(() -> takeOver.set(startTakingOver(session, second)));

        stream(threads, session, first);
        threads.joinAll();
        takeOver.get().thread().join();

        assertThat(first.messages).extracting(ReceivedMessage::position).containsExactly("p@0");
        assertThat(second.messages).extracting(ReceivedMessage::position).containsExactly("p@1", "p@2");
    }

    @Test
    void letsTheLatestClientWinWhenTwoTakeOverAtOnce() throws InterruptedException {
        var session = openSession();
        var executor = new CapturingExecutor();
        stream(executor, session, new RecordingSink());
        var earlier = startTakingOver(session, new RecordingSink().leavingAfterMessages(0));
        var later = startTakingOver(session, new RecordingSink().leavingAfterMessages(0));

        earlier.thread().join();
        executor.runAll();
        later.thread().join();

        assertThat(earlier.failure().get()).isInstanceOf(ConsumeSessionInUseException.class);
        assertThat(later.failure().get()).isNull();
    }

    @Test
    void rejectsATakeOverWhenTheStreamIsNotHandedOverInTime() {
        var session = openSession();
        stream(new CapturingExecutor(), session, new RecordingSink());
        handoverTimeout = Duration.ofMillis(100);

        assertThatThrownBy(() -> stream(SAME_THREAD, session, new RecordingSink()))
                .isInstanceOf(ConsumeSessionInUseException.class)
                .hasFieldOrPropertyWithValue("code", "messaging.session-in-use");
    }

    @Test
    void failsATakeOverWhenTheSessionClosesMeanwhile() throws InterruptedException {
        var session = openSession();
        stream(new CapturingExecutor(), session, new RecordingSink());
        var takeOver = startTakingOver(session, new RecordingSink());

        close.execute(new CloseConsumeSessionCommand(session));
        takeOver.thread().join();

        assertThat(takeOver.failure().get()).isInstanceOf(ConsumeSessionNotFoundException.class);
    }

    @Test
    void failsForAnUnknownSession() {
        assertThatThrownBy(() -> stream(SAME_THREAD, ConsumeSessionId.newId(), new RecordingSink()))
                .isInstanceOf(ConsumeSessionNotFoundException.class);
    }

    @Test
    void reportsAReadFailureAndClosesTheSession() {
        var session = openSession();
        kafka.lastStream().willFail(new ReadFailedException("Connection lost"));
        var sink = new RecordingSink();

        stream(SAME_THREAD, session, sink);

        assertThat(sink.failure).hasMessage("Connection lost");
        assertThat(kafka.lastStream().isClosed()).isTrue();
        assertThat(registry.contains(session)).isFalse();
    }

    @Test
    void endsTheStreamWhenTheSessionIsClosedWhileStreaming() {
        var session = openSession();
        var executor = new CapturingExecutor();
        var sink = new RecordingSink();
        stream(executor, session, sink);
        close.execute(new CloseConsumeSessionCommand(session));
        assertThat(kafka.lastStream().isClosed())
                .as("the streaming thread owns the broker stream until it stops")
                .isFalse();

        executor.runAll();

        assertThat(sink.closed).isTrue();
        assertThat(kafka.lastStream().isClosed()).isTrue();
    }

    @AfterEach
    void stopStreaming() throws InterruptedException {
        registry.close();
        threads.joinAll();
    }

    private void stream(Executor executor, ConsumeSessionId session, ConsumeSessionSink sink) {
        new StreamConsumeSessionUseCase(registry, executor, clock, handoverTimeout)
                .execute(new StreamConsumeSessionCommand(session, sink));
    }

    /** A client connecting from its own thread, which is waiting for the stream when this returns. */
    private TakeOver startTakingOver(ConsumeSessionId session, ConsumeSessionSink sink) {
        var failure = new AtomicReference<@Nullable RuntimeException>();
        var thread = Thread.startVirtualThread(() -> {
            try {
                stream(SAME_THREAD, session, sink);
            } catch (RuntimeException e) {
                failure.set(e);
            }
        });
        await().until(() -> thread.getState() == Thread.State.TIMED_WAITING);
        return new TakeOver(thread, failure);
    }

    private record TakeOver(Thread thread, AtomicReference<@Nullable RuntimeException> failure) {}

    private ConsumeSessionId openSession() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:9092");
        return open.execute(
                        new OpenConsumeSessionCommand(connection, Destination.topic("orders"), StartPosition.LATEST))
                .id();
    }
}
