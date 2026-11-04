package com.oriolvc.topicman.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.shared.domain.BrokerType;
import org.junit.jupiter.api.Test;

class TlsSettingsTest {

    @Test
    void isDisabledWithTheDefaultCasByDefault() {
        assertThat(TlsSettings.disabled().enabled()).isFalse();
        assertThat(TlsSettings.disabled().trustedCertificates()).isNull();
    }

    @Test
    void kafkaCanSwitchTlsOn() {
        var tls = new TlsSettings(true, new TrustedCertificates(Pem.CA));

        assertThat(tls.forBroker(BrokerType.KAFKA)).isEqualTo(tls);
    }

    @Test
    void pulsarAndRabbitMqSwitchTlsOnWithTheirAddressNotTheFlag() {
        var enabled = new TlsSettings(true, null);

        assertThatThrownBy(() -> enabled.forBroker(BrokerType.PULSAR))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining("pulsar+ssl://");
        assertThatThrownBy(() -> enabled.forBroker(BrokerType.RABBITMQ))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining("amqps://");
    }

    @Test
    void everyBrokerCanTrustItsOwnCertificates() {
        var tls = new TlsSettings(false, new TrustedCertificates(Pem.CA));

        for (var brokerType : BrokerType.values()) {
            assertThat(tls.forBroker(brokerType)).isEqualTo(tls);
        }
    }
}
