package com.oriolvc.topicman.messaging.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class BrokersApiIT {

    private final MockMvcTester mvc;

    BrokersApiIT(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void listsEveryBrokerWithItsCapabilities() {
        var result = mvc.get().uri("/api/v1/brokers").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().isStrictlyEqualTo("""
                [
                  {"type": "PULSAR", "destinationType": "TOPIC", "canReadFromBeginning": true, "supportsMessageKeys": true},
                  {"type": "KAFKA", "destinationType": "TOPIC", "canReadFromBeginning": true, "supportsMessageKeys": true},
                  {"type": "RABBITMQ", "destinationType": "EXCHANGE", "canReadFromBeginning": false, "supportsMessageKeys": false}
                ]
                """);
    }
}
