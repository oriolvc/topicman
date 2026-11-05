package com.oriolvc.topicman.collection.application.usecase;

import static com.oriolvc.topicman.collection.application.usecase.CollectionFixtures.CLOCK;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.collection.application.model.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.application.model.EnvironmentResult;
import com.oriolvc.topicman.collection.application.model.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListEnvironmentsUseCaseTest {

    private final InMemoryEnvironmentRepository repository = new InMemoryEnvironmentRepository();

    @Test
    void listsEnvironmentsOrderedByName() {
        var create = new CreateEnvironmentUseCase(repository, CLOCK);
        create.execute(new CreateEnvironmentCommand(new EnvironmentName("staging"), List.of()));
        create.execute(new CreateEnvironmentCommand(new EnvironmentName("dev"), List.of()));

        var result = new ListEnvironmentsUseCase(repository).execute(new ListEnvironmentsCommand());

        assertThat(result).extracting(EnvironmentResult::name).containsExactly("dev", "staging");
    }
}
