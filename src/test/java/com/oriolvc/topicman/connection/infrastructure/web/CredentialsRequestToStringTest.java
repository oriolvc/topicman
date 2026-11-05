package com.oriolvc.topicman.connection.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.connection.infrastructure.web.model.BrokerType;
import com.oriolvc.topicman.connection.infrastructure.web.model.CreateConnectionRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.SaslMechanism;
import com.oriolvc.topicman.connection.infrastructure.web.model.TokenCredentialsRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.UpdateConnectionRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.UsernamePasswordCredentialsRequest;
import org.junit.jupiter.api.Test;

/**
 * Logging a request must never print a credential. The generated DTOs mask the properties the spec
 * marks with {@code format: password}, so this guards that marking.
 */
class CredentialsRequestToStringTest {

    private static final String PASSWORD = "pa55-word-value";
    private static final String TOKEN = "jwt-token-value";

    private static UsernamePasswordCredentialsRequest aUsernamePasswordRequest() {
        return new UsernamePasswordCredentialsRequest(
                        UsernamePasswordCredentialsRequest.TypeEnum.USERNAME_PASSWORD, "app", PASSWORD)
                .saslMechanism(SaslMechanism.SCRAM_SHA_256);
    }

    private static TokenCredentialsRequest aTokenRequest() {
        return new TokenCredentialsRequest(TokenCredentialsRequest.TypeEnum.TOKEN, TOKEN);
    }

    @Test
    void masksThePasswordButShowsTheUsername() {
        var text = aUsernamePasswordRequest().toString();

        assertThat(text).doesNotContain(PASSWORD).contains("password: *").contains("username: app");
    }

    @Test
    void masksTheToken() {
        assertThat(aTokenRequest().toString()).doesNotContain(TOKEN).contains("token: *");
    }

    @Test
    void masksCredentialsInsideACreateRequest() {
        var kafka =
                new CreateConnectionRequest("kafka", BrokerType.KAFKA, "localhost:9092", aUsernamePasswordRequest());
        var pulsar =
                new CreateConnectionRequest("pulsar", BrokerType.PULSAR, "pulsar://localhost:6650", aTokenRequest());

        assertThat(kafka.toString()).doesNotContain(PASSWORD);
        assertThat(pulsar.toString()).doesNotContain(TOKEN);
    }

    @Test
    void masksCredentialsInsideAnUpdateRequest() {
        assertThat(new UpdateConnectionRequest()
                        .credentials(aUsernamePasswordRequest())
                        .toString())
                .doesNotContain(PASSWORD);
        assertThat(new UpdateConnectionRequest().credentials(aTokenRequest()).toString())
                .doesNotContain(TOKEN);
    }
}
