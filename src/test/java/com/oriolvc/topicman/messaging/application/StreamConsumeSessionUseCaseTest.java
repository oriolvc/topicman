package com.oriolvc.topicman.messaging.application;

import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.NOW;
import static com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.aReceivedText;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.application.ConsumeSessionFixtures.CapturingExecutor;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionId;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionInUseException;
import com.oriolvc.topicman.messaging.domain.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.PayloadFormat;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
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
    void rejectsASecondClientWhileOneIsStreaming() {
        var session = openSession();
        stream(new CapturingExecutor(), session, new RecordingSink());

        assertThatThrownBy(() -> stream(SAME_THREAD, session, new RecordingSink()))
                .isInstanceOf(ConsumeSessionInUseException.class)
                .hasFieldOrPropertyWithValue("code", "messaging.session-in-use");
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

    private void stream(Executor executor, ConsumeSessionId session, ConsumeSessionSink sink) {
        new StreamConsumeSessionUseCase(registry, executor, clock)
                .execute(new StreamConsumeSessionCommand(session, sink));
    }

    private ConsumeSessionId openSession() {
        var connection = endpoints.add(BrokerType.KAFKA, "localhost:9092");
        return open.execute(
                        new OpenConsumeSessionCommand(connection, Destination.topic("orders"), StartPosition.LATEST))
                .id();
    }
}
