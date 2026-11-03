package com.oriolvc.topicman.collection.application;

import static com.oriolvc.topicman.collection.application.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.TestIds;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeleteEnvironmentUseCaseTest {

    private final InMemoryEnvironmentRepository repository = new InMemoryEnvironmentRepository();
    private final DeleteEnvironmentUseCase useCase = new DeleteEnvironmentUseCase(repository);

    @Test
    void deletesAnEnvironment() {
        var created = new CreateEnvironmentUseCase(repository, CLOCK)
                .execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of()));

        useCase.execute(new DeleteEnvironmentCommand(new EnvironmentId(created.id())));

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void failsForAnUnknownEnvironment() {
        assertThatThrownBy(() -> useCase.execute(new DeleteEnvironmentCommand(TestIds.environmentId())))
                .isInstanceOf(EnvironmentNotFoundException.class);
    }
}
