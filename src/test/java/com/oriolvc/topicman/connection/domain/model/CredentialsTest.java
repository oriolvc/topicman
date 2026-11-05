package com.oriolvc.topicman.connection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import org.junit.jupiter.api.Test;

class CredentialsTest {

    @Test
    void noCredentialsAreSupportedByEveryBroker() {
        for (var brokerType : BrokerType.values()) {
            assertThat(Credentials.none().forBroker(brokerType)).isEqualTo(Credentials.none());
        }
    }

    @Test
    void tokensAreSupportedOnlyByPulsar() {
        var token = Credentials.token(new Secret("jwt"));

        assertThat(token.forBroker(BrokerType.PULSAR)).isEqualTo(token);
        assertThatThrownBy(() -> token.forBroker(BrokerType.KAFKA)).isInstanceOf(InvalidConnectionException.class);
        assertThatThrownBy(() -> token.forBroker(BrokerType.RABBITMQ)).isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void usernameAndPasswordAreNotSupportedByPulsar() {
        var usernamePassword = Credentials.usernamePassword("app", new Secret("pw"));

        assertThatThrownBy(() -> usernamePassword.forBroker(BrokerType.PULSAR))
                .isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void kafkaUsesTheGivenSaslMechanism() {
        var scram = Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_512);

        assertThat(scram.forBroker(BrokerType.KAFKA)).isEqualTo(scram);
    }

    @Test
    void kafkaDefaultsToSaslPlain() {
        var usernamePassword = Credentials.usernamePassword("app", new Secret("pw"));

        assertThat(usernamePassword.forBroker(BrokerType.KAFKA))
                .isEqualTo(Credentials.usernamePassword("app", new Secret("pw"), SaslMechanism.PLAIN));
    }

    @Test
    void rabbitMqTakesAUsernameAndPasswordWithoutASaslMechanism() {
        var usernamePassword = Credentials.usernamePassword("guest", new Secret("guest"));

        assertThat(usernamePassword.forBroker(BrokerType.RABBITMQ)).isEqualTo(usernamePassword);
    }

    @Test
    void rabbitMqRejectsASaslMechanism() {
        var scram = Credentials.usernamePassword("guest", new Secret("guest"), SaslMechanism.SCRAM_SHA_256);

        assertThatThrownBy(() -> scram.forBroker(BrokerType.RABBITMQ))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining("SASL mechanism");
    }

    @Test
    void stripsSurroundingWhitespaceFromTheUsername() {
        var credentials = Credentials.usernamePassword(" app ", new Secret("pw"));

        assertThat(((Credentials.UsernamePassword) credentials).username()).isEqualTo("app");
    }

    @Test
    void rejectsBlankUsername() {
        assertThatThrownBy(() -> Credentials.usernamePassword(" ", new Secret("pw")))
                .isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void neverShowsSecretsInToString() {
        var token = Credentials.token(new Secret("jwt-value"));
        var usernamePassword = Credentials.usernamePassword("app", new Secret("pw-value"), SaslMechanism.PLAIN);

        assertThat(token.toString()).doesNotContain("jwt-value");
        assertThat(usernamePassword.toString()).doesNotContain("pw-value").contains("app");
    }
}
