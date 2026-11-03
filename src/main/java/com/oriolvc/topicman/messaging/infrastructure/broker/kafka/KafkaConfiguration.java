package com.oriolvc.topicman.messaging.infrastructure.broker.kafka;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaProperties.class)
class KafkaConfiguration {

    @Bean
    KafkaMessageProducer kafkaMessageProducer(KafkaProperties properties) {
        return new KafkaMessageProducer(properties);
    }

    @Bean
    KafkaConnectionTester kafkaConnectionTester(KafkaProperties properties) {
        return new KafkaConnectionTester(properties);
    }

    @Bean
    KafkaMessageReader kafkaMessageReader(KafkaProperties properties) {
        return new KafkaMessageReader(properties);
    }
}
