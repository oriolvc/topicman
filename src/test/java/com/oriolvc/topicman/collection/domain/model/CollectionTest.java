package com.oriolvc.topicman.collection.domain.model;

import static com.oriolvc.topicman.collection.domain.model.MessageTemplateTest.aTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.exception.TemplateNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.TemplateNotFoundException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CollectionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-03T10:00:00Z");
    private static final Instant LATER = CREATED_AT.plusSeconds(60);

    @Test
    void startsEmpty() {
        var collection = Collection.create(TestIds.collectionId(), new CollectionName("Orders"), CREATED_AT);

        assertThat(collection.name()).isEqualTo(new CollectionName("Orders"));
        assertThat(collection.templates()).isEmpty();
        assertThat(collection.createdAt()).isEqualTo(CREATED_AT);
        assertThat(collection.updatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void keepsTemplatesInTheOrderTheyWereAdded() {
        var collection = anEmptyCollection();
        var created = aTemplate("Order created");
        var shipped = aTemplate("Order shipped");

        collection.addTemplate(created, LATER);
        collection.addTemplate(shipped, LATER);

        assertThat(collection.templates()).containsExactly(created, shipped);
        assertThat(collection.template(shipped.id())).contains(shipped);
        assertThat(collection.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void rejectsASecondTemplateWithTheSameName() {
        var collection = anEmptyCollection();
        collection.addTemplate(aTemplate("Order created"), LATER);

        assertThatThrownBy(() -> collection.addTemplate(aTemplate("Order created"), LATER))
                .isInstanceOf(TemplateNameTakenException.class);
        assertThat(collection.templates()).hasSize(1);
    }

    @Test
    void replacesATemplateInPlace() {
        var collection = anEmptyCollection();
        var created = aTemplate("Order created");
        var shipped = aTemplate("Order shipped");
        collection.addTemplate(created, CREATED_AT);
        collection.addTemplate(shipped, CREATED_AT);
        var edited = created.withContent(
                created.name(),
                BrokerType.KAFKA,
                TemplateDestination.topic("orders-v2"),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "edited"));

        collection.replaceTemplate(edited, LATER);

        assertThat(collection.templates()).containsExactly(edited, shipped);
        assertThat(collection.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void rejectsRenamingATemplateToAnotherTemplatesName() {
        var collection = anEmptyCollection();
        var created = aTemplate("Order created");
        collection.addTemplate(created, CREATED_AT);
        collection.addTemplate(aTemplate("Order shipped"), CREATED_AT);
        var renamed = created.withContent(
                new TemplateName("Order shipped"),
                created.brokerType(),
                created.destination(),
                created.key(),
                created.headers(),
                created.payload());

        assertThatThrownBy(() -> collection.replaceTemplate(renamed, LATER))
                .isInstanceOf(TemplateNameTakenException.class);
    }

    @Test
    void failsToReplaceOrRemoveATemplateItDoesNotHave() {
        var collection = anEmptyCollection();
        var stranger = aTemplate("Elsewhere");

        assertThatThrownBy(() -> collection.replaceTemplate(stranger, LATER))
                .isInstanceOf(TemplateNotFoundException.class);
        assertThatThrownBy(() -> collection.removeTemplate(stranger.id(), LATER))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void removesATemplate() {
        var collection = anEmptyCollection();
        var created = aTemplate("Order created");
        collection.addTemplate(created, CREATED_AT);

        collection.removeTemplate(created.id(), LATER);

        assertThat(collection.templates()).isEmpty();
        assertThat(collection.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void createsACollectionWithTemplatesFromAFile() {
        var created = aTemplate("Order created");
        var shipped = aTemplate("Order shipped");

        var collection = Collection.fromFile(
                TestIds.collectionId(), new CollectionName("Orders"), List.of(created, shipped), CREATED_AT);

        assertThat(collection.templates()).containsExactly(created, shipped);
        assertThat(collection.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void rejectsAFileWithTwoTemplatesOfTheSameNameAsInvalid() {
        var twice = List.of(aTemplate("Order created"), aTemplate("Order created"));

        assertThatThrownBy(() ->
                        Collection.fromFile(TestIds.collectionId(), new CollectionName("Orders"), twice, CREATED_AT))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("Order created");
    }

    @Test
    void renames() {
        var collection = anEmptyCollection();

        collection.rename(new CollectionName("Shipping"), LATER);

        assertThat(collection.name()).isEqualTo(new CollectionName("Shipping"));
        assertThat(collection.updatedAt()).isEqualTo(LATER);
    }

    @Test
    void exposesTemplatesReadOnly() {
        var collection = anEmptyCollection();

        assertThatThrownBy(() -> collection.templates().add(aTemplate("Sneaky")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Collection anEmptyCollection() {
        return Collection.create(TestIds.collectionId(), new CollectionName("Orders"), CREATED_AT);
    }
}
