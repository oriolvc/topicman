package com.oriolvc.topicman.messaging.infrastructure.session;

import com.oriolvc.topicman.messaging.application.ConsumeSessionLimits;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumeSessionProperties.class)
class ConsumeSessionConfiguration {

    @Bean
    ConsumeSessionLimits consumeSessionLimits(ConsumeSessionProperties properties) {
        return new ConsumeSessionLimits(properties.maxOpen(), properties.idleTimeout());
    }
}
