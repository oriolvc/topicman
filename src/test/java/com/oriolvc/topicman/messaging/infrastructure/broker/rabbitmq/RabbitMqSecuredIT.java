package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.messaging.domain.Authentication;
import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.MessageProducer;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.Tls;
import com.oriolvc.topicman.messaging.infrastructure.broker.SecuredBrokerContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.Secret;
import java.time.Duration;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** The RabbitMQ adapter against a broker serving AMQP over TLS to a user with a password (ADR 0008). */
@Testcontainers
class RabbitMqSecuredIT extends SecuredBrokerContractTest {

    private static final String USER = "app";
    private static final String PASSWORD = "app-secret";

    @Container
    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.2-management")
            .withAdminUser(USER)
            .withAdminPassword(PASSWORD)
            .withCopyToContainer(Transferable.of(TestCertificates.pem("ca.pem"), 0644), "/etc/rabbitmq/tls/ca.pem")
            .withCopyToContainer(
                    Transferable.of(TestCertificates.pem("server.pem"), 0644), "/etc/rabbitmq/tls/server.pem")
            .withCopyToContainer(
                    Transferable.of(TestCertificates.pem("server-key.pem"), 0644), "/etc/rabbitmq/tls/server-key.pem")
            .withCopyToContainer(Transferable.of("""
                            listeners.ssl.default = 5671
                            ssl_options.cacertfile = /etc/rabbitmq/tls/ca.pem
                            ssl_options.certfile = /etc/rabbitmq/tls/server.pem
                            ssl_options.keyfile = /etc/rabbitmq/tls/server-key.pem
                            ssl_options.verify = verify_none
                            ssl_options.fail_if_no_peer_cert = false
                            """, 0644), "/etc/rabbitmq/conf.d/20-tls.conf");

    private static final RabbitMqProperties PROPERTIES =
            new RabbitMqProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static RabbitMqConnections connections;

    @BeforeAll
    static void connect() {
        connections = new RabbitMqConnections(PROPERTIES);
    }

    @AfterAll
    static void disconnect() {
        connections.close();
    }

    @Override
    protected MessageProducer createProducer() {
        return new RabbitMqMessageProducer(connections, PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new RabbitMqMessageReader(connections);
    }

    @Override
    protected ConnectionTester createTester() {
        return new RabbitMqConnectionTester(connections);
    }

    @Override
    protected BrokerEndpoint endpoint(Authentication authentication, @Nullable String trustedCertificates) {
        return new BrokerEndpoint(
                BrokerType.RABBITMQ,
                "amqps://localhost:" + RABBITMQ.getAmqpsPort(),
                authentication,
                new Tls(false, trustedCertificates));
    }

    @Override
    protected Authentication validAuthentication() {
        return new Authentication.UsernamePassword(USER, new Secret(PASSWORD), null);
    }

    @Override
    protected Authentication rejectedAuthentication() {
        return new Authentication.UsernamePassword(USER, new Secret("wrong-secret"), null);
    }

    @Override
    protected Destination newDestination() {
        // A built-in exchange, so no setup connection is needed; each test has its own routing key.
        return Destination.exchange("amq.topic", "secured-" + UUID.randomUUID());
    }

    @Test
    void usesTheEndpointCredentialsOverTheUriOnes() {
        var endpoint = new BrokerEndpoint(
                BrokerType.RABBITMQ,
                "amqps://someone:not-the-password@localhost:" + RABBITMQ.getAmqpsPort(),
                validAuthentication(),
                new Tls(false, TestCertificates.CA));

        var outcome = tester.test(endpoint);

        assertThat(outcome.succeeded()).as(outcome.detail()).isTrue();
    }
}
