package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PulsarServiceUrlProbeTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "pulsar://localhost:6650             | localhost:6650",
                "pulsar://broker                     | broker:6650",
                "pulsar+ssl://broker                 | broker:6651",
                "http://broker:8080/                 | broker:8080",
                "https://broker                      | broker:443",
                "pulsar://[::1]                      | [::1]:6650",
                "pulsar://[::1]:6660                 | [::1]:6660",
                "pulsar://a:6650, b:6651,c           | a:6650;b:6651;c:6650",
            })
    void listsTheHostsOfAServiceUrlWithDefaultPorts(String serviceUrl, String expected) {
        var servers = PulsarServiceUrlProbe.servers(serviceUrl);

        assertThat(servers).containsExactly(expected.split(";"));
    }
}
