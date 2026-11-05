package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.exception.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.Headers;
import com.oriolvc.topicman.messaging.domain.model.Message;
import com.oriolvc.topicman.messaging.domain.model.Payload;
import com.oriolvc.topicman.messaging.domain.model.SendReceipt;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.messaging.domain.port.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.port.MessageProducer;
import com.oriolvc.topicman.messaging.domain.port.MessageReader;
import com.oriolvc.topicman.messaging.infrastructure.broker.BrokerAdapterContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaAdapterContractIT extends BrokerAdapterContractTest {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private static final KafkaProperties PROPERTIES =
            new KafkaProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static Admin admin;

    // A plain client, to write records the adapter's producer cannot (empty keys, null headers).
    private static KafkaProducer<String, byte[]> rawProducer;

    @BeforeAll
    static void connect() {
        admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()));
        rawProducer = new KafkaProducer<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
                new StringSerializer(),
                new ByteArraySerializer());
    }

    @AfterAll
    static void disconnect() {
        rawProducer.close();
        admin.close();
    }

    @Override
    protected BrokerType brokerType() {
        return BrokerType.KAFKA;
    }

    @Override
    protected MessageProducer createProducer() {
        return new KafkaMessageProducer(PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new KafkaMessageReader(PROPERTIES);
    }

    @Override
    protected ConnectionTester createTester() {
        return new KafkaConnectionTester(PROPERTIES);
    }

    @Override
    protected BrokerEndpoint endpoint() {
        return new BrokerEndpoint(BrokerType.KAFKA, KAFKA.getBootstrapServers());
    }

    @Override
    protected BrokerEndpoint unreachableEndpoint() {
        return new BrokerEndpoint(BrokerType.KAFKA, "localhost:1");
    }

    @Override
    protected Destination newDestination() {
        // One partition, because Kafka orders messages only within a partition.
        return Destination.topic(createTopic(1));
    }

    @Override
    protected Destination missingDestination() {
        return Destination.topic("missing-" + UUID.randomUUID());
    }

    @Override
    protected Destination unsupportedDestination() {
        return Destination.exchange("orders", "created");
    }

    @Override
    protected void assertNoReaderStateLeftFor(Destination destination) {
        try {
            assertThat(admin.listGroups().all().get()).isEmpty();
        } catch (Exception e) {
            throw new IllegalStateException("Could not list consumer groups", e);
        }
    }

    @Test
    void usesThePartitionAndOffsetAsReceiptAndPosition() {
        var topic = createTopic(1);

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.LATEST)) {
            var first = producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("first")));
            var second = producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("second")));

            assertThat(first).isEqualTo(new SendReceipt(topic + "-0@0"));
            assertThat(second).isEqualTo(new SendReceipt(topic + "-0@1"));
            assertThat(pollUntil(stream, 2))
                    .extracting(received -> received.position())
                    .containsExactly(topic + "-0@0", topic + "-0@1");
        }
    }

    @Test
    void readsFromEveryPartition() {
        var topic = createTopic(3);
        for (int partition = 0; partition < 3; partition++) {
            produceRaw(new ProducerRecord<>(topic, partition, null, bytes("p" + partition)));
        }

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            assertThat(payloadsOf(pollUntil(stream, 3))).containsExactlyInAnyOrder("p0", "p1", "p2");
        }
    }

    @Test
    void readsTheRecordTimestamp() {
        var topic = createTopic(1);
        var timestamp = Instant.parse("2026-10-16T10:00:00.123Z");
        produceRaw(new ProducerRecord<>(topic, 0, timestamp.toEpochMilli(), null, bytes("hello")));

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            assertThat(pollUntil(stream, 1).getFirst().timestamp()).isEqualTo(timestamp);
        }
    }

    @Test
    void toleratesRecordsWithEmptyKeysAndHeaderValuesOrBlankHeaderNames() {
        var topic = createTopic(1);
        var record = new ProducerRecord<>(topic, "", bytes("odd"));
        record.headers().add("no-value", null);
        record.headers().add(" ", bytes("blank name"));
        produceRaw(record);

        try (var stream = reader.open(endpoint(), Destination.topic(topic), StartPosition.EARLIEST)) {
            var received = pollUntil(stream, 1).getFirst();

            assertThat(received.message().key()).isNull();
            assertThat(received.message().headers()).isEqualTo(Headers.of(Map.of("no-value", "")));
        }
    }

    @Test
    void doesNotCreateAMissingTopicWhenReadingIt() throws Exception {
        var topic = "missing-" + UUID.randomUUID();

        assertThatThrownBy(() -> reader.open(endpoint(), Destination.topic(topic), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageContaining(topic);
        assertThat(admin.listTopics().names().get()).doesNotContain(topic);
    }

    @Test
    void failsOnAnInvalidAddress() {
        var invalid = new BrokerEndpoint(BrokerType.KAFKA, "not-an-address");

        assertThatThrownBy(() -> producer.send(invalid, Destination.topic("orders"), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);
        assertThatThrownBy(() -> reader.open(invalid, Destination.topic("orders"), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class);
    }

    @Test
    void namesTheBootstrapServersItCannotProduceTo() {
        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "localhost:1,localhost:2");

        assertThatThrownBy(() -> producer.send(endpoint, newDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageStartingWith("Cannot reach the Kafka broker at localhost:1,localhost:2: ");
    }

    @Test
    void namesTheBootstrapServersItCannotReadFrom() {
        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "localhost:1,localhost:2");

        assertThatThrownBy(() -> reader.open(endpoint, newDestination(), StartPosition.LATEST))
                .isInstanceOf(ReadFailedException.class)
                .hasMessageStartingWith("Cannot reach the Kafka broker at localhost:1,localhost:2: ");
    }

    @Test
    void namesTheClusterAndItsBrokersWhenTheTestPasses() {
        var outcome = tester.test(endpoint());

        assertThat(outcome.detail()).matches("Kafka cluster \\S+, 1 broker");
    }

    @Test
    void failsTheTestWhenTheBrokerAdvertisesAnAddressThisServerCannotReach() {
        try (var misconfigured = new GenericContainer<>("apache/kafka:4.2.1")
                .withExposedPorts(9092)
                .withEnv(Map.of(
                        "KAFKA_NODE_ID", "1",
                        "KAFKA_PROCESS_ROLES", "broker,controller",
                        "KAFKA_CONTROLLER_QUORUM_VOTERS", "1@localhost:9093",
                        "KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER",
                        "KAFKA_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093",
                        // The mistake: clients are told to use an address only containers resolve.
                        "KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://unreachable.invalid:9092",
                        "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1"))
                .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1))) {
            misconfigured.start();
            var endpoint = new BrokerEndpoint(
                    BrokerType.KAFKA, misconfigured.getHost() + ":" + misconfigured.getMappedPort(9092));

            var outcome = tester.test(endpoint);

            assertThat(outcome.succeeded()).isFalse();
            assertThat(outcome.detail()).contains("advertised");
        }
    }

    @Test
    void producesAgainAfterAFailedSend() {
        var topic = Destination.topic(createTopic(1));
        assertThatThrownBy(() -> producer.send(unreachableEndpoint(), topic, Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);

        var receipt = producer.send(endpoint(), topic, Message.of(Payload.text("ok")));

        assertThat(receipt.reference()).endsWith("-0@0");
    }

    private static String createTopic(int partitions) {
        var topic = "topic-" + UUID.randomUUID();
        try {
            admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 1)))
                    .all()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create topic " + topic, e);
        }
        return topic;
    }

    private static void produceRaw(ProducerRecord<String, byte[]> record) {
        try {
            rawProducer.send(record).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not produce to " + record.topic(), e);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(UTF_8);
    }
}
