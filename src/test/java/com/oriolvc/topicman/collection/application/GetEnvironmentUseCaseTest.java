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

class GetEnvironmentUseCaseTest {

    private final InMemoryEnvironmentRepository repository = new InMemoryEnvironmentRepository();
    private final GetEnvironmentUseCase useCase = new GetEnvironmentUseCase(repository);

    @Test
    void getsAnEnvironment() {
        var created = new CreateEnvironmentUseCase(repository, CLOCK)
                .execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of()));

        var result = useCase.execute(new GetEnvironmentCommand(new EnvironmentId(created.id())));

        assertThat(result.name()).isEqualTo("staging");
    }

    @Test
    void failsForAnUnknownEnvironment() {
        assertThatThrownBy(() -> useCase.execute(new GetEnvironmentCommand(TestIds.environmentId())))
                .isInstanceOf(EnvironmentNotFoundException.class);
    }
}
