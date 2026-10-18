package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.SendReceipt;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@Testcontainers
class KafkaMessageProducerIT {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.2.1");

    private final KafkaMessageProducer producer =
            new KafkaMessageProducer(new KafkaProperties(Duration.ofSeconds(3), Duration.ofSeconds(3)));

    @AfterEach
    void closeProducer() {
        producer.close();
    }

    @Test
    void producesKeyHeadersAndPayload() {
        var topic = aTopic();
        var message = Message.of(Payload.json("{\"id\":42}"))
                .withKey("order-42")
                .withHeaders(Headers.of(Map.of("source", "topicman")));

        producer.send(endpoint(), Destination.topic(topic), message);

        var record = readSingleRecord(topic);
        assertThat(record.key()).isEqualTo("order-42");
        assertThat(new String(record.value(), UTF_8)).isEqualTo("{\"id\":42}");
        assertThat(headersOf(record)).containsExactly(Map.entry("source", "topicman"));
    }

    @Test
    void producesAMessageWithoutKey() {
        var topic = aTopic();

        producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("hello")));

        var record = readSingleRecord(topic);
        assertThat(record.key()).isNull();
        assertThat(new String(record.value(), UTF_8)).isEqualTo("hello");
    }

    @Test
    void returnsThePartitionAndOffsetAsReceipt() {
        var topic = aTopic();

        var first = producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("first")));
        var second = producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("second")));

        assertThat(first).isEqualTo(new SendReceipt(topic + "-0@0"));
        assertThat(second).isEqualTo(new SendReceipt(topic + "-0@1"));
    }

    @Test
    void rejectsAnExchangeDestination() {
        assertThatThrownBy(() -> producer.send(
                        endpoint(), Destination.exchange("orders", "created"), Message.of(Payload.text("x"))))
                .isInstanceOf(InvalidDestinationException.class);
    }

    @Test
    void failsWhenTheBrokerCannotBeReached() {
        var unreachable = new BrokerEndpoint(BrokerType.KAFKA, "localhost:1");

        assertThatThrownBy(() -> producer.send(unreachable, Destination.topic("orders"), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .hasMessageContaining("orders");
    }

    @Test
    void failsWhenTheAddressIsInvalid() {
        var invalid = new BrokerEndpoint(BrokerType.KAFKA, "not-an-address");

        assertThatThrownBy(() -> producer.send(invalid, Destination.topic("orders"), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);
    }

    @Test
    void recoversAfterAFailedSend() {
        var topic = aTopic();
        var unreachable = new BrokerEndpoint(BrokerType.KAFKA, "localhost:1");
        assertThatThrownBy(() -> producer.send(unreachable, Destination.topic(topic), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class);

        var receipt = producer.send(endpoint(), Destination.topic(topic), Message.of(Payload.text("ok")));

        assertThat(receipt).isEqualTo(new SendReceipt(topic + "-0@0"));
    }

    private static BrokerEndpoint endpoint() {
        return new BrokerEndpoint(BrokerType.KAFKA, KAFKA.getBootstrapServers());
    }

    private static String aTopic() {
        var topic = "topic-" + UUID.randomUUID();
        try (var admin =
                Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create topic " + topic, e);
        }
        return topic;
    }

    private static ConsumerRecord<String, byte[]> readSingleRecord(String topic) {
        var partition = new TopicPartition(topic, 0);
        var records = new ArrayList<ConsumerRecord<String, byte[]>>();
        try (var consumer = new KafkaConsumer<>(
                Map.<String, Object>of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()),
                new StringDeserializer(),
                new ByteArrayDeserializer())) {
            consumer.assign(List.of(partition));
            consumer.seekToBeginning(List.of(partition));
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(records::add);
                assertThat(records).isNotEmpty();
            });
        }
        assertThat(records).hasSize(1);
        return records.getFirst();
    }

    private static Map<String, String> headersOf(ConsumerRecord<?, ?> record) {
        var headers = new LinkedHashMap<String, String>();
        for (Header header : record.headers()) {
            headers.put(header.key(), new String(header.value(), UTF_8));
        }
        return headers;
    }
}
