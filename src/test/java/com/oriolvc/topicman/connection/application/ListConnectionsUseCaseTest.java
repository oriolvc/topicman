package com.oriolvc.topicman.connection.application;

import static com.oriolvc.topicman.connection.application.ConnectionFixtures.CLOCK;
import static com.oriolvc.topicman.connection.application.ConnectionFixtures.aCreateKafkaConnectionCommand;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ListConnectionsUseCaseTest {

    private final InMemoryConnectionRepository repository = new InMemoryConnectionRepository();
    private final CreateConnectionUseCase create = new CreateConnectionUseCase(repository, CLOCK);
    private final ListConnectionsUseCase useCase = new ListConnectionsUseCase(repository);

    @Test
    void listsConnectionsOrderedByName() {
        create.execute(aCreateKafkaConnectionCommand("Staging"));
        create.execute(aCreateKafkaConnectionCommand("Local"));

        var results = useCase.execute(new ListConnectionsCommand());

        assertThat(results).extracting(ConnectionResult::name).containsExactly("Local", "Staging");
    }

    @Test
    void listsNothingWhenThereAreNoConnections() {
        assertThat(useCase.execute(new ListConnectionsCommand())).isEmpty();
    }
}
