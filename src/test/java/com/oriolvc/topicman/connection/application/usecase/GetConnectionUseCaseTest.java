package com.oriolvc.topicman.connection.application.usecase;

import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.usecase.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.application.model.GetConnectionCommand;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.domain.TestConnectionIds;
import org.junit.jupiter.api.Test;

class GetConnectionUseCaseTest {

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase create = new CreateConnectionUseCase(repository, CLOCK);
    private final GetConnectionUseCase useCase = new GetConnectionUseCase(repository);

    @Test
    void getsAConnection() {
        var created = create.execute(aCreateKafkaConnectionCommand("Local"));

        var result = useCase.execute(new GetConnectionCommand(new ConnectionId(created.id())));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void failsForAnUnknownConnection() {
        assertThatThrownBy(() -> useCase.execute(new GetConnectionCommand(TestConnectionIds.next())))
                .isInstanceOf(ConnectionNotFoundException.class)
                .hasFieldOrPropertyWithValue("code", "connection.not-found");
    }
}
