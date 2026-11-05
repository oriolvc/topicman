package com.oriolvc.topicman.connection.application.usecase;

import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.application.model.DeleteConnectionCommand;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import org.junit.jupiter.api.Test;

class DeleteConnectionUseCaseTest {

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase create = new CreateConnectionUseCase(repository, CLOCK);
    private final DeleteConnectionUseCase useCase = new DeleteConnectionUseCase(repository);

    @Test
    void deletesAConnection() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        useCase.execute(new DeleteConnectionCommand(new ConnectionId(created.id())));

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(new DeleteConnectionCommand(TestConnectionIds.next())))
                .isInstanceOf(ConnectionNotFoundException.class);
    }
}
