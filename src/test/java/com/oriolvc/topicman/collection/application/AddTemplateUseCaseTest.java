package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.PayloadFormat;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplateNameTakenException;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.collection.domain.TestIds;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AddTemplateUseCaseTest {

    private final InMemoryCollectionRepository repository = new InMemoryCollectionRepository();
    private final FakeSupportedBrokers brokers = new FakeSupportedBrokers();
    private final AddTemplateUseCase useCase = new AddTemplateUseCase(repository, brokers, CLOCK);
    private final CollectionId collection = new CollectionId(new CreateCollectionUseCase(repository, CLOCK)
            .execute(new CreateCollectionCommand(new CollectionName("Orders")))
            .id());

    @Test
    void addsATemplateAtTheEnd() {
        useCase.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")));

        var result = useCase.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order shipped")));

        assertThat(result.name()).isEqualTo("Order shipped");
        assertThat(result.key()).isEqualTo("{{orderId}}");
        assertThat(repository.findById(collection).orElseThrow().templates())
                .extracting(template -> template.name().value())
                .containsExactly("Order created", "Order shipped");
    }

    @Test
    void rejectsATemplateThatDoesNotFitItsBroker() {
        var topicForRabbitMq = new TemplateContent(
                new TemplateName("Wrong kind"),
                BrokerType.RABBITMQ,
                TemplateDestination.topic("orders"),
                null,
                Map.of(),
                new TemplatePayload(PayloadFormat.TEXT, "x"));

        assertThatThrownBy(() -> useCase.execute(new AddTemplateCommand(collection, topicForRabbitMq)))
                .isInstanceOf(InvalidTemplateException.class);
        assertThat(repository.findById(collection).orElseThrow().templates()).isEmpty();
    }

    @Test
    void rejectsATemplateForABrokerThisServerCannotProduceTo() {
        brokers.remove(BrokerType.KAFKA);

        assertThatThrownBy(() -> useCase.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created"))))
                .isInstanceOf(InvalidTemplateException.class)
                .hasMessageContaining("KAFKA");
    }

    @Test
    void rejectsATakenTemplateName() {
        useCase.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")));

        assertThatThrownBy(() -> useCase.execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created"))))
                .isInstanceOf(TemplateNameTakenException.class);
    }

    @Test
    void failsForAnUnknownCollection() {
        assertThatThrownBy(() -> useCase.execute(
                        new AddTemplateCommand(TestIds.collectionId(), aKafkaTemplate("Order created"))))
                .isInstanceOf(CollectionNotFoundException.class);
    }
}
