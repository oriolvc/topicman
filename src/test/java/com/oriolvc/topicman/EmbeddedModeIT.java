package com.oriolvc.topicman;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.connection.application.model.BrokerCredentials;
import com.oriolvc.topicman.connection.application.model.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.model.GetBrokerAccessCommand;
import com.oriolvc.topicman.connection.application.usecase.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.application.usecase.GetBrokerAccessUseCase;
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
                .run("--server.port=0", "--topicman.crypto.master-key=", "--topicman.data-dir=" + dataDirectory);
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
