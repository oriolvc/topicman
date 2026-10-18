package com.oriolvc.topicman.connection.application;

import static com.oriolvc.topicman.connection.application.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.ConnectionId;
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
        assertThatThrownBy(() -> useCase.execute(new DeleteConnectionCommand(ConnectionId.newId())))
                .isInstanceOf(ConnectionNotFoundException.class);
    }
}
