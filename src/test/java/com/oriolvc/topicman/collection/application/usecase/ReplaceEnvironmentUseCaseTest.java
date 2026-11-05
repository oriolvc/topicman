package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.application.model.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.application.model.ReplaceEnvironmentCommand;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import com.oriolvc.topicman.collection.domain.model.TestIds;
import com.oriolvc.topicman.collection.domain.model.Variable;
import com.oriolvc.topicman.collection.domain.model.VariableName;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReplaceEnvironmentUseCaseTest {

    private final InMemoryEnvironmentRepository repository = new InMemoryEnvironmentRepository();
    private final CreateEnvironmentUseCase create = new CreateEnvironmentUseCase(repository, CLOCK);
    private final ReplaceEnvironmentUseCase useCase = new ReplaceEnvironmentUseCase(repository, CLOCK);

    @Test
    void replacesTheNameAndVariables() {
        var created = create.execute(new CreateEnvironmentCommand(
                new EnvironmentName("staging"), List.of(new Variable(new VariableName("env"), "stg"))));

        var result = useCase.execute(new ReplaceEnvironmentCommand(
                new EnvironmentId(created.id()),
                new EnvironmentName("stage"),
                List.of(new Variable(new VariableName("region"), "eu"))));

        assertThat(result.name()).isEqualTo("stage");
        assertThat(result.variables()).containsExactly(Map.entry("region", "eu"));
    }

    @Test
    void rejectsAnotherEnvironmentsName() {
        var staging = create.execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of()));
        create.execute(new CreateEnvironmentCommand(new EnvironmentName("dev"), List.of()));

        assertThatThrownBy(() -> useCase.execute(new ReplaceEnvironmentCommand(
                        new EnvironmentId(staging.id()), new EnvironmentName("dev"), List.of())))
                .isInstanceOf(EnvironmentNameTakenException.class);
    }

    @Test
    void failsForAnUnknownEnvironment() {
        assertThatThrownBy(() -> useCase.execute(
                        new ReplaceEnvironmentCommand(TestIds.environmentId(), new EnvironmentName("dev"), List.of())))
                .isInstanceOf(EnvironmentNotFoundException.class);
    }
}
