package com.oriolvc.topicman;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Tests run on PostgreSQL. The {@code embeddedDatabaseTest} Gradle task sets
 * {@code topicman.test.database=h2}, so the same tests run on the default embedded database (ADR 0010).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ConditionalOnProperty(name = "topicman.test.database", havingValue = "postgresql", matchIfMissing = true)
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }
}
