package com.oriolvc.topicman.messaging.infrastructure.broker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.messaging.domain.exception.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.Message;
import com.oriolvc.topicman.messaging.domain.model.Payload;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.model.StartPosition;
import com.oriolvc.topicman.messaging.domain.port.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.port.MessageProducer;
import com.oriolvc.topicman.messaging.domain.port.MessageReader;
import java.time.Duration;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What every broker adapter must do against a broker that requires TLS and authentication (ADR
 * 0008). The broker's certificate is {@code server.pem} from {@code src/test/resources/tls/},
 * issued by the test CA for localhost.
 */
public abstract class SecuredBrokerContractTest {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    protected MessageProducer producer;
    protected MessageReader reader;
    protected ConnectionTester tester;

    protected abstract MessageProducer createProducer();

    protected abstract MessageReader createReader();

    protected abstract ConnectionTester createTester();

    /**
     * An endpoint of the secured broker with TLS switched on, as the broker type does it (Kafka's
     * flag, or the address scheme), trusting the given certificates.
     */
    protected abstract BrokerEndpoint endpoint(Authentication authentication, @Nullable String trustedCertificates);

    /** Credentials the broker accepts. */
    protected abstract Authentication validAuthentication();

    /** Credentials the broker rejects: a wrong password or token. */
    protected abstract Authentication rejectedAuthentication();

    /** A new destination that exists on the broker. */
    protected abstract Destination newDestination();

    protected final BrokerEndpoint securedEndpoint() {
        return endpoint(validAuthentication(), TestCertificates.CA);
    }

    @BeforeEach
    void createAdapters() {
        producer = createProducer();
        reader = createReader();
        tester = createTester();
    }

    @AfterEach
    void closeAdapters() throws Exception {
        if (producer instanceof AutoCloseable closeable) {
            closeable.close();
        }
        if (reader instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }

    @Test
    void producesAndReadsOverTlsWithCredentials() {
        var destination = newDestination();

        try (var stream = reader.open(securedEndpoint(), destination, StartPosition.LATEST)) {
            producer.send(securedEndpoint(), destination, Message.of(Payload.text("secured")));

            var received = new ArrayList<ReceivedMessage>();
            await().atMost(READ_TIMEOUT).untilAsserted(() -> {
                received.addAll(stream.poll(Duration.ofMillis(200)));
                assertThat(received).isNotEmpty();
            });
            assertThat(received.getFirst().message().payload().content()).isEqualTo("secured");
        }
    }

    @Test
    void passesTheConnectionTestWithTrustedCertificatesAndValidCredentials() {
        var outcome = tester.test(securedEndpoint());

        assertThat(outcome.succeeded()).as(outcome.detail()).isTrue();
    }

    @Test
    void failsTheConnectionTestWithRejectedCredentialsWithoutShowingThem() {
        var endpoint = endpoint(rejectedAuthentication(), TestCertificates.CA);

        var outcome = tester.test(endpoint);

        assertThat(outcome.succeeded()).isFalse();
        assertThat(outcome.detail()).doesNotContain(secretOf(rejectedAuthentication()));
    }

    @Test
    void failsToProduceWithRejectedCredentials() {
        var endpoint = endpoint(rejectedAuthentication(), TestCertificates.CA);

        assertThatThrownBy(() -> producer.send(endpoint, newDestination(), Message.of(Payload.text("x"))))
                .isInstanceOf(ProduceFailedException.class)
                .message()
                .doesNotContain(secretOf(rejectedAuthentication()));
    }

    @Test
    void failsTheConnectionTestWhenTheBrokerCertificateIsNotTrusted() {
        var outcome = tester.test(endpoint(validAuthentication(), TestCertificates.OTHER_CA));

        assertThat(outcome.succeeded()).isFalse();
    }

    @Test
    void failsTheConnectionTestWhenTheCertificateIsForAnotherHost() {
        var trusted = securedEndpoint();
        assertThat(trusted.address()).contains("localhost");
        // The same broker, through another loopback address that its certificate does not name.
        var otherHost = new BrokerEndpoint(
                trusted.type(),
                trusted.address().replace("localhost", "127.0.0.2"),
                trusted.authentication(),
                trusted.tls());

        var outcome = tester.test(otherHost);

        assertThat(outcome.succeeded()).isFalse();
    }

    @Test
    void doesNotTrustAPrivateCaThroughTheJvmDefaults() {
        var outcome = tester.test(endpoint(validAuthentication(), null));

        assertThat(outcome.succeeded()).isFalse();
    }

    private static String secretOf(Authentication authentication) {
        return switch (authentication) {
            case Authentication.None none -> throw new IllegalArgumentException("No secret");
            case Authentication.Token token -> token.token().value();
            case Authentication.UsernamePassword usernamePassword ->
                usernamePassword.password().value();
        };
    }
}
