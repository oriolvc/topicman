package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.CollectionFixtures.aKafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateId;
import com.oriolvc.topicman.collection.domain.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.TestIds;
import com.oriolvc.topicman.collection.domain.UnresolvedVariablesException;
import com.oriolvc.topicman.collection.domain.Variable;
import com.oriolvc.topicman.collection.domain.VariableName;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResolveTemplateUseCaseTest {

    private final InMemoryCollectionRepository collections = new InMemoryCollectionRepository();
    private final InMemoryEnvironmentRepository environments = new InMemoryEnvironmentRepository();
    private final ResolveTemplateUseCase useCase =
            new ResolveTemplateUseCase(collections, environments, new FixedDynamicValues());
    private final CollectionId collection = new CollectionId(new CreateCollectionUseCase(collections, CLOCK)
            .execute(new CreateCollectionCommand(new CollectionName("Orders")))
            .id());
    private final TemplateId template =
            new TemplateId(new AddTemplateUseCase(collections, new FakeSupportedBrokers(), CLOCK)
                    .execute(new AddTemplateCommand(collection, aKafkaTemplate("Order created")))
                    .id());

    @Test
    void resolvesATemplateAgainstAnEnvironment() {
        var staging = anEnvironment(
                "staging",
                new Variable(new VariableName("env"), "stg"),
                new Variable(new VariableName("orderId"), "42"));

        var result = useCase.execute(new ResolveTemplateCommand(collection, template, staging));

        assertThat(result.destination()).isEqualTo(TemplateDestination.topic("orders-stg"));
        assertThat(result.key()).isEqualTo("42");
        assertThat(result.payload().content())
                .isEqualTo("{\"id\": \"42\", \"trace\": \"" + FixedDynamicValues.UUID_VALUE + "\"}");
    }

    @Test
    void listsTheMissingVariablesWithoutAnEnvironment() {
        assertThatThrownBy(() -> useCase.execute(new ResolveTemplateCommand(collection, template, null)))
                .isInstanceOfSatisfying(
                        UnresolvedVariablesException.class,
                        failure -> assertThat(failure.missing()).containsExactly("env", "orderId"));
    }

    @Test
    void failsForAnUnknownEnvironment() {
        assertThatThrownBy(() ->
                        useCase.execute(new ResolveTemplateCommand(collection, template, TestIds.environmentId())))
                .isInstanceOf(EnvironmentNotFoundException.class);
    }

    @Test
    void failsForAnUnknownTemplate() {
        assertThatThrownBy(() -> useCase.execute(new ResolveTemplateCommand(collection, TestIds.templateId(), null)))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    private EnvironmentId anEnvironment(String name, Variable... variables) {
        return new EnvironmentId(new CreateEnvironmentUseCase(environments, CLOCK)
                .execute(new CreateEnvironmentCommand(new EnvironmentName(name), List.of(variables)))
                .id());
    }
}
