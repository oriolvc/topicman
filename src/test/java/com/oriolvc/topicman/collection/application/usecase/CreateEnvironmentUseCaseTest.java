package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import com.oriolvc.topicman.collection.domain.model.Variable;
import com.oriolvc.topicman.collection.domain.model.VariableName;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateEnvironmentUseCaseTest {

    private final InMemoryEnvironmentRepository repository = new InMemoryEnvironmentRepository();
    private final CreateEnvironmentUseCase useCase = new CreateEnvironmentUseCase(repository, CLOCK);

    @Test
    void createsAnEnvironmentWithItsVariablesInOrder() {
        var result = useCase.execute(new CreateEnvironmentCommand(
                new EnvironmentName("staging"),
                List.of(new Variable(new VariableName("env"), "stg"), new Variable(new VariableName("a"), "1"))));

        assertThat(result.name()).isEqualTo("staging");
        assertThat(result.variables()).containsExactly(Map.entry("env", "stg"), Map.entry("a", "1"));
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void rejectsATakenName() {
        useCase.execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of()));

        assertThatThrownBy(
                        () -> useCase.execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of())))
                .isInstanceOf(EnvironmentNameTakenException.class);
    }

    @Test
    void rejectsTheSameVariableTwice() {
        var twice = List.of(new Variable(new VariableName("env"), "stg"), new Variable(new VariableName("env"), "prd"));

        assertThatThrownBy(() -> useCase.execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), twice)))
                .isInstanceOf(InvalidEnvironmentException.class);
    }
}
