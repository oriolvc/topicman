package com.oriolvc.topicman.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import org.junit.jupiter.api.Test;

class CredentialsTest {

    @Test
    void noCredentialsAreSupportedByEveryBroker() {
        assertThat(BrokerType.values()).allMatch(Credentials.none()::isSupportedBy);
    }

    @Test
    void tokensAreSupportedOnlyByPulsar() {
        var token = Credentials.token(new Secret("jwt"));

        assertThat(token.isSupportedBy(BrokerType.PULSAR)).isTrue();
        assertThat(token.isSupportedBy(BrokerType.KAFKA)).isFalse();
        assertThat(token.isSupportedBy(BrokerType.RABBITMQ)).isFalse();
    }

    @Test
    void usernameAndPasswordAreSupportedByKafkaAndRabbitMq() {
        var usernamePassword = Credentials.usernamePassword("app", new Secret("pw"));

        assertThat(usernamePassword.isSupportedBy(BrokerType.KAFKA)).isTrue();
        assertThat(usernamePassword.isSupportedBy(BrokerType.RABBITMQ)).isTrue();
        assertThat(usernamePassword.isSupportedBy(BrokerType.PULSAR)).isFalse();
    }

    @Test
    void stripsSurroundingWhitespaceFromTheUsername() {
        var credentials = new Credentials.UsernamePassword(" app ", new Secret("pw"));

        assertThat(credentials.username()).isEqualTo("app");
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> Credentials.usernamePassword(" ", new Secret("pw")))
                .isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void neverShowsSecretsInToString() {
        var token = Credentials.token(new Secret("jwt-value"));
        var usernamePassword = Credentials.usernamePassword("app", new Secret("pw-value"));

        assertThat(token.toString()).doesNotContain("jwt-value");
        assertThat(usernamePassword.toString()).doesNotContain("pw-value").contains("app");
    }
}
