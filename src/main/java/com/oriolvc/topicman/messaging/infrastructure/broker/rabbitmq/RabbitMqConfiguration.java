package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RabbitMqProperties.class)
class RabbitMqConfiguration {

    @Bean
    RabbitMqConnections rabbitMqConnections(RabbitMqProperties properties) {
        return new RabbitMqConnections(properties);
    }

    @Bean
    RabbitMqMessageProducer rabbitMqMessageProducer(RabbitMqConnections connections, RabbitMqProperties properties) {
        return new RabbitMqMessageProducer(connections, properties);
    }

    @Bean
    RabbitMqMessageReader rabbitMqMessageReader(RabbitMqConnections connections) {
        return new RabbitMqMessageReader(connections);
    }
}
