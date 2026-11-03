package com.oriolvc.topicman.collection.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.collection.domain.Collection;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.MessageTemplate;
import com.oriolvc.topicman.collection.domain.PayloadFormat;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

@DataJpaTest
@TestConstructor(autowireMode = AutowireMode.ALL)
@Import({TestcontainersConfiguration.class, JpaCollectionRepository.class})
class JpaCollectionRepositoryIT {

    private static final Instant CREATED_AT = Instant.parse("2026-11-03T10:00:00.123456Z");
    private static final Instant LATER = CREATED_AT.plusSeconds(60);

    private final JpaCollectionRepository repository;
    private final EntityManager entityManager;

    JpaCollectionRepositoryIT(JpaCollectionRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void savesAndFindsACollectionWithItsTemplatesInOrder() {
        var collection = Collection.create(new CollectionName("Orders"), CREATED_AT);
        var headers = new LinkedHashMap<String, String>();
        headers.put("z-last-alphabetically", "{{$uuid}}");
        headers.put("a-first-alphabetically", "topicman");
        var kafka = MessageTemplate.create(
                new TemplateName("Order created"),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders-{{env}}"),
                "{{orderId}}",
                headers,
                new TemplatePayload(PayloadFormat.JSON, "{\"id\": \"{{orderId}}\"}"));
        var rabbitMq = MessageTemplate.create(
                new TemplateName("Broadcast"),
                BrokerType.RABBITMQ,
                TemplateDestination.exchange("events", ""),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, ""));
        collection.addTemplate(kafka, CREATED_AT);
        collection.addTemplate(rabbitMq, CREATED_AT);

        repository.save(collection);
        flushAndClear();

        assertThat(repository.findById(collection.id())).hasValueSatisfying(found -> {
            assertThat(found.name()).isEqualTo(new CollectionName("Orders"));
            assertThat(found.createdAt()).isEqualTo(CREATED_AT);
            assertThat(found.templates()).containsExactly(kafka, rabbitMq);
            assertThat(found.templates().getFirst().headers().keySet())
                    .containsExactly("z-last-alphabetically", "a-first-alphabetically");
        });
    }

    @Test
    void updatesTemplatesKeepingTheirIdsAndOrder() {
        var collection = Collection.create(new CollectionName("Orders"), CREATED_AT);
        var created = aTemplate("Order created");
        var shipped = aTemplate("Order shipped");
        collection.addTemplate(created, CREATED_AT);
        collection.addTemplate(shipped, CREATED_AT);
        repository.save(collection);
        flushAndClear();
        var edited = created.withContent(
                new TemplateName("Order placed"),
                BrokerType.PULSAR,
                TemplateDestination.topic("placed"),
                null,
                Map.of("only", "one"),
                new TemplatePayload(PayloadFormat.TEXT, "placed"));
        var cancelled = aTemplate("Order cancelled");
        collection.replaceTemplate(edited, LATER);
        collection.removeTemplate(shipped.id(), LATER);
        collection.addTemplate(cancelled, LATER);

        repository.save(collection);
        flushAndClear();

        assertThat(repository.findById(collection.id())).hasValueSatisfying(found -> {
            assertThat(found.templates()).containsExactly(edited, cancelled);
            assertThat(found.updatedAt()).isEqualTo(LATER);
        });
        assertThat(rowCount("message_template")).isEqualTo(2);
    }

    @Test
    void listsCollectionsByName() {
        repository.save(Collection.create(new CollectionName("Shipping"), CREATED_AT));
        repository.save(Collection.create(new CollectionName("Billing"), CREATED_AT));
        flushAndClear();

        assertThat(repository.findAll())
                .extracting(found -> found.name().value())
                .containsExactly("Billing", "Shipping");
        assertThat(repository.existsByName(new CollectionName("Billing"))).isTrue();
        assertThat(repository.existsByName(new CollectionName("Missing"))).isFalse();
    }

    @Test
    void rejectsATakenName() {
        repository.save(Collection.create(new CollectionName("Orders"), CREATED_AT));

        assertThatThrownBy(() -> repository.save(Collection.create(new CollectionName("Orders"), CREATED_AT)))
                .isInstanceOf(CollectionNameTakenException.class);
    }

    @Test
    void deletesACollectionWithItsTemplatesAndHeaders() {
        var collection = Collection.create(new CollectionName("Orders"), CREATED_AT);
        collection.addTemplate(aTemplate("Order created"), CREATED_AT);
        repository.save(collection);
        flushAndClear();

        repository.deleteById(collection.id());
        flushAndClear();

        assertThat(repository.findById(collection.id())).isEmpty();
        assertThat(rowCount("message_template")).isZero();
        assertThat(rowCount("message_template_header")).isZero();
    }

    private static MessageTemplate aTemplate(String name) {
        return MessageTemplate.create(
                new TemplateName(name),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders"),
                "k",
                Map.of("source", "topicman"),
                new TemplatePayload(PayloadFormat.TEXT, name));
    }

    private long rowCount(String table) {
        return ((Number) entityManager
                        .createNativeQuery("SELECT COUNT(*) FROM " + table)
                        .getSingleResult())
                .longValue();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
