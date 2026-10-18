package com.oriolvc.topicman.messaging.infrastructure.web;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ConsumeSessionExecutorConfiguration {

    /**
     * One virtual thread per streamed session. Shut down with {@code shutdownNow} so the interrupt
     * ends every stream loop instead of waiting for clients to leave.
     */
    @Bean(destroyMethod = "shutdownNow")
    ExecutorService consumeSessionExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("consume-session-", 0).factory());
    }
}
