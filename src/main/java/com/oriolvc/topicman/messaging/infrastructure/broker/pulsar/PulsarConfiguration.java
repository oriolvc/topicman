package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PulsarProperties.class)
class PulsarConfiguration {

    @Bean
    PulsarClients pulsarClients(PulsarProperties properties) {
        return new PulsarClients(properties);
    }

    @Bean
    PulsarMessageProducer pulsarMessageProducer(PulsarClients clients, PulsarProperties properties) {
        return new PulsarMessageProducer(clients, properties);
    }

    @Bean
    PulsarMessageReader pulsarMessageReader(PulsarClients clients, PulsarProperties properties) {
        return new PulsarMessageReader(clients, properties);
    }
}
