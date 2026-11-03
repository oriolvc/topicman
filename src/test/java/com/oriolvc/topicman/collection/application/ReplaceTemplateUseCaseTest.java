package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.PayloadFormat;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateId;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.collection.domain.TestIds;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReplaceTemplateUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final FakeSupportedBrokers brokers = new FakeSupportedBrokers();
    private final ReplaceTemplateUseCase useCase = new ReplaceTemplateUseCase(repository, brokers, CLOCK);
    private final CollectionId collection = new CollectionId(new CreateCollectionUseCase(repository, CLOCK)
            .execute(new CreateCollectionCommand(new CollectionName("Orders")))
            .id());
    private final AddTemplateUseCase add = new AddTemplateUseCase(repository, brokers, CLOCK);

    @Test
    void replacesATemplateKeepingItsIdAndPlace() {
        var first = add.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")));
        add.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order shipped")));
        var rabbitMq = new TemplateContent(
                new TemplateName("Order created on RabbitMQ"),
                BrokerType.RABBITMQ,
                TemplateDestination.exchange("orders", "created"),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "created"));

        var result = useCase.execute(new ReplaceTemplateCommand(collection, new TemplateId(first.id()), rabbitMq));

        assertThat(result.id()).isEqualTo(first.id());
        assertThat(result.brokerType()).isEqualTo(BrokerType.RABBITMQ);
        assertThat(repository.findById(collection).orElseThrow().templates())
                .extracting(template -> template.name().value())
                .containsExactly("Order created on RabbitMQ", "Order shipped");
    }

    @Test
    void rejectsAReplacementThatDoesNotFitItsBroker() {
        var first = add.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")));
        var keyedRabbitMq = new TemplateContent(
                new TemplateName("Keyed"),
                BrokerType.RABBITMQ,
                TemplateDestination.exchange("orders", ""),
                "order-42",
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "x"));

        assertThatThrownBy(() -> useCase.execute(
                        new ReplaceTemplateCommand(collection, new TemplateId(first.id()), keyedRabbitMq)))
                .isInstanceOf(InvalidTemplateException.class);
    }

    @Test
    void failsForAnUnknownTemplate() {
        assertThatThrownBy(() -> useCase.execute(
                        new ReplaceTemplateCommand(collection, TestIds.templateId(), aKafkaTemplate("Anything"))))
                .isInstanceOf(TemplateNotFoundException.class);
    }
}
