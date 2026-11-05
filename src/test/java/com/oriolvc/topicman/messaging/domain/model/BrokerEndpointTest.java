package com.oriolvc.topicman.messaging.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.exception.InvalidBrokerEndpointException;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import org.junit.jupiter.api.Test;

class BrokerEndpointTest {

    @Test
    void keepsTheBrokerTypeAndAddress() {
        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092");

        assertThat(endpoint.type()).isEqualTo(BrokerType.KAFKA);
        assertThat(endpoint.address()).isEqualTo("localhost:9092");
    }

    @Test
    void hasNoAuthenticationAndDefaultTlsUnlessGiven() {
        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "localhost:9092");

        assertThat(endpoint.authentication()).isEqualTo(Authentication.none());
        assertThat(endpoint.tls()).isEqualTo(Tls.defaults());
        assertThat(endpoint.tls().enabled()).isFalse();
        assertThat(endpoint.tls().trustedCertificates()).isNull();
    }

    @Test
    void keepsAuthenticationAndTls() {
        var authentication = new Authentication.UsernamePassword("app", new Secret("pw"), SaslMechanism.SCRAM_SHA_256);
        var tls = new Tls(true, "-----BEGIN CERTIFICATE-----");

        var endpoint = new BrokerEndpoint(BrokerType.KAFKA, "kafka:9093", authentication, tls);

        assertThat(endpoint.authentication()).isEqualTo(authentication);
        assertThat(endpoint.tls()).isEqualTo(tls);
    }

    @Test
    void neverShowsSecretsInToString() {
        var endpoint = new BrokerEndpoint(
                BrokerType.PULSAR,
                "pulsar+ssl://pulsar:6651",
                new Authentication.Token(new Secret("jwt-value")),
                Tls.defaults());

        assertThat(endpoint.toString()).doesNotContain("jwt-value");
    }

    @Test
    void rejectsBlankAddress() {
        assertThatThrownBy(() -> new BrokerEndpoint(BrokerType.KAFKA, " "))
                .isInstanceOf(InvalidBrokerEndpointException.class);
    }
}
