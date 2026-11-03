package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.NOW;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.PayloadFormat;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ImportCollectionUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final FakeSupportedBrokers brokers = new FakeSupportedBrokers();
    private final ImportCollectionUseCase useCase = new ImportCollectionUseCase(repository, brokers, CLOCK);

    @Test
    void createsACollectionWithTheTemplatesOfTheFileInOrder() {
        var result = useCase.execute(new ImportCollectionCommand(
                new CollectionName("Orders"),
                List.of(aKafkaTemplate("Order created"), aKafkaTemplate("Order shipped"))));

        assertThat(result.name()).isEqualTo("Orders");
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(repository
                        .findById(new CollectionId(result.id()))
                        .orElseThrow()
                        .templates())
                .extracting(template -> template.name().value())
                .containsExactly("Order created", "Order shipped");
    }

    @Test
    void rejectsATakenName() {
        useCase.execute(new ImportCollectionCommand(new CollectionName("Orders"), List.of()));

        assertThatThrownBy(() -> useCase.execute(new ImportCollectionCommand(new CollectionName("Orders"), List.of())))
                .isInstanceOf(CollectionNameTakenException.class);
    }

    @Test
    void createsNothingWhenATemplateDoesNotFitItsBroker() {
        var keyedRabbitMq = new TemplateContent(
                new TemplateName("Keyed"),
                BrokerType.RABBITMQ,
                TemplateDestination.exchange("events", ""),
                "k",
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "x"));

        assertThatThrownBy(() -> useCase.execute(new ImportCollectionCommand(
                        new CollectionName("Orders"), List.of(aKafkaTemplate("Order created"), keyedRabbitMq))))
                .isInstanceOf(InvalidTemplateException.class);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void createsNothingWhenTwoTemplatesHaveTheSameName() {
        var twice = List.of(aKafkaTemplate("Order created"), aKafkaTemplate("Order created"));

        assertThatThrownBy(() -> useCase.execute(new ImportCollectionCommand(new CollectionName("Orders"), twice)))
                .isInstanceOf(InvalidTemplateException.class);
        assertThat(repository.findAll()).isEmpty();
    }
}
