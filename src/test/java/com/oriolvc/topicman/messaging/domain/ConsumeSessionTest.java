package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConsumeSessionTest {

    private static final Instant OPENED_AT = Instant.parse("2026-10-16T10:00:00Z");

    @Test
    void opensWithTheRequestedStartPosition() {
        var connectionId = ConnectionId.newId();

        var session = aKafkaSessionFrom(connectionId, StartPosition.EARLIEST);

        assertThat(session.connectionId()).isEqualTo(connectionId);
        assertThat(session.destination()).isEqualTo(Destination.topic("orders"));
        assertThat(session.startPosition()).isEqualTo(StartPosition.EARLIEST);
        assertThat(session.openedAt()).isEqualTo(OPENED_AT);
        assertThat(session.lastActivityAt()).isEqualTo(OPENED_AT);
        assertThat(session.isOpen()).isTrue();
    }

    @Test
    void givesEachSessionItsOwnIdentity() {
        var connectionId = ConnectionId.newId();

        var first = aKafkaSessionFrom(connectionId, StartPosition.LATEST);
        var second = aKafkaSessionFrom(connectionId, StartPosition.LATEST);

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsDestinationTheBrokerCannotRead() {
        assertThatThrownBy(() -> ConsumeSession.open(
                        ConnectionId.newId(),
                        BrokerType.RABBITMQ,
                        Destination.topic("orders"),
                        StartPosition.LATEST,
                        OPENED_AT))
                .isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void recordsActivity() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        var later = OPENED_AT.plusSeconds(30);

        session.recordActivity(later);

        assertThat(session.lastActivityAt()).isEqualTo(later);
    }

    @Test
    void ignoresActivityOlderThanTheLastOne() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        session.recordActivity(OPENED_AT.plusSeconds(30));

        session.recordActivity(OPENED_AT.plusSeconds(10));

        assertThat(session.lastActivityAt()).isEqualTo(OPENED_AT.plusSeconds(30));
    }

    @Test
    void closes() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        var closedAt = OPENED_AT.plusSeconds(60);

        session.close(closedAt);

        assertThat(session.isOpen()).isFalse();
        assertThat(session.closedAt()).isEqualTo(closedAt);
    }

    @Test
    void closingTwiceKeepsTheFirstCloseTime() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        session.close(OPENED_AT.plusSeconds(60));

        session.close(OPENED_AT.plusSeconds(90));

        assertThat(session.closedAt()).isEqualTo(OPENED_AT.plusSeconds(60));
    }

    @Test
    void rejectsActivityAfterClose() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        session.close(OPENED_AT.plusSeconds(60));

        assertThatThrownBy(() -> session.recordActivity(OPENED_AT.plusSeconds(90)))
                .isInstanceOf(ConsumeSessionClosedException.class);
    }

    @Test
    void isNotIdleBeforeTheTimeout() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);

        assertThat(session.isIdle(
                        Duration.ofMinutes(5),
                        OPENED_AT.plus(Duration.ofMinutes(5)).minusMillis(1)))
                .isFalse();
    }

    @Test
    void isIdleOnceTheTimeoutPassedWithoutActivity() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);

        assertThat(session.isIdle(Duration.ofMinutes(5), OPENED_AT.plus(Duration.ofMinutes(5))))
                .isTrue();
    }

    @Test
    void countsIdleTimeFromTheLastActivity() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        session.recordActivity(OPENED_AT.plus(Duration.ofMinutes(4)));

        assertThat(session.isIdle(Duration.ofMinutes(5), OPENED_AT.plus(Duration.ofMinutes(6))))
                .isFalse();
    }

    @Test
    void aClosedSessionIsNotIdle() {
        var session = aKafkaSessionFrom(ConnectionId.newId(), StartPosition.LATEST);
        session.close(OPENED_AT);

        assertThat(session.isIdle(Duration.ofMinutes(5), OPENED_AT.plus(Duration.ofHours(1))))
                .isFalse();
    }

    private static ConsumeSession aKafkaSessionFrom(ConnectionId connectionId, StartPosition startPosition) {
        return ConsumeSession.open(
                connectionId, BrokerType.KAFKA, Destination.topic("orders"), startPosition, OPENED_AT);
    }
}
