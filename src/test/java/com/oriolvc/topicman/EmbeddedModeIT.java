package com.oriolvc.topicman;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.collection.application.model.DeleteCollectionCommand;
import com.oriolvc.topicman.collection.application.model.DeleteEnvironmentCommand;
import com.oriolvc.topicman.collection.application.model.ListCollectionsCommand;
import com.oriolvc.topicman.collection.application.model.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.application.usecase.DeleteCollectionUseCase;
import com.oriolvc.topicman.collection.application.usecase.DeleteEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListCollectionsUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListEnvironmentsUseCase;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.connection.application.model.BrokerCredentials;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.model.DeleteConnectionCommand;
import com.oriolvc.topicman.connection.application.model.GetBrokerAccessCommand;
import com.oriolvc.topicman.connection.application.model.ListConnectionsCommand;
import com.oriolvc.topicman.connection.application.usecase.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.application.usecase.DeleteConnectionUseCase;
import com.oriolvc.topicman.connection.application.usecase.GetBrokerAccessUseCase;
import com.oriolvc.topicman.connection.application.usecase.ListConnectionsUseCase;
import com.oriolvc.topicman.connection.domain.model.BrokerAddress;
import com.oriolvc.topicman.connection.domain.model.ConnectionName;
import com.oriolvc.topicman.connection.domain.model.Credentials;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.Secret;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** The app with no configuration at all: no database and no master key (ADR 0010). */
class EmbeddedModeIT {

    @TempDir
    private Path dataDirectory;

    private ConfigurableApplicationContext start() {
        // Arguments beat src/test/resources/config, which sets a master key for the other tests.
        return new SpringApplicationBuilder(TopicManApplication.class)
                .run(
                        "--server.port=0",
                        "--topicman.crypto.master-key=",
                        "--topicman.seed-examples=",
                        "--topicman.data-dir=" + dataDirectory);
    }

    @Test
    void loadsTheExamplesOnceSoDeletedOnesDoNotComeBack() {
        try (var context = start()) {
            var connections = context.getBean(ListConnectionsUseCase.class).execute(new ListConnectionsCommand());
            var collections = context.getBean(ListCollectionsUseCase.class).execute(new ListCollectionsCommand());
            var environments = context.getBean(ListEnvironmentsUseCase.class).execute(new ListEnvironmentsCommand());
            assertThat(connections).isNotEmpty();
            assertThat(collections).isNotEmpty();
            assertThat(environments).isNotEmpty();
            connections.forEach(connection -> context.getBean(DeleteConnectionUseCase.class)
                    .execute(new DeleteConnectionCommand(new ConnectionId(connection.id()))));
            collections.forEach(collection -> context.getBean(DeleteCollectionUseCase.class)
                    .execute(new DeleteCollectionCommand(new CollectionId(collection.id()))));
            environments.forEach(environment -> context.getBean(DeleteEnvironmentUseCase.class)
                    .execute(new DeleteEnvironmentCommand(new EnvironmentId(environment.id()))));
        }

        try (var context = start()) {
            assertThat(context.getBean(ListConnectionsUseCase.class).execute(new ListConnectionsCommand()))
                    .isEmpty();
            assertThat(context.getBean(ListCollectionsUseCase.class).execute(new ListCollectionsCommand()))
                    .isEmpty();
            assertThat(context.getBean(ListEnvironmentsUseCase.class).execute(new ListEnvironmentsCommand()))
                    .isEmpty();
        }
    }

    @Test
    void startsWithNoConfigurationAndKeepsCredentialsReadableAcrossRestarts() {
        long id;
        try (var context = start()) {
            assertThat(dataDirectory.resolve("master.key")).exists();
            assertThat(dataDirectory.resolve("topicman.mv.db")).exists();
            id = context.getBean(CreateConnectionUseCase.class)
                    .execute(new CreateConnectionCommand(
                            new ConnectionName("pulsar"),
                            BrokerType.PULSAR,
                            new BrokerAddress("pulsar://localhost:6650"),
                            Credentials.token(new Secret("the-token"))))
                    .id();
        }

        try (var context = start()) {
            var access = context.getBean(GetBrokerAccessUseCase.class)
                    .execute(new GetBrokerAccessCommand(new ConnectionId(id)));

            assertThat(access.credentials())
                    .isInstanceOfSatisfying(
                            BrokerCredentials.Token.class,
                            token -> assertThat(token.token().value()).isEqualTo("the-token"));
        }
    }
}
