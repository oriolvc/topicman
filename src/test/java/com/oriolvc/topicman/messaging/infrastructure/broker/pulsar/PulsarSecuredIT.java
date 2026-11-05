package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.model.Destination;
import com.oriolvc.topicman.messaging.domain.model.Tls;
import com.oriolvc.topicman.messaging.domain.port.ConnectionTester;
import com.oriolvc.topicman.messaging.domain.port.MessageProducer;
import com.oriolvc.topicman.messaging.domain.port.MessageReader;
import com.oriolvc.topicman.messaging.infrastructure.broker.SecuredBrokerContractTest;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.Secret;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.pulsar.PulsarContainer;

/**
 * The Pulsar adapter against a standalone broker that requires a JWT on every connection and
 * serves TLS on {@code pulsar+ssl://} (ADR 0008).
 */
@Testcontainers
class PulsarSecuredIT extends SecuredBrokerContractTest {

    private static final int TLS_PORT = 6651;
    private static final byte[] TOKEN_KEY = "topicman-test-only-token-signing-key!!".getBytes(UTF_8);
    private static final String ADMIN_TOKEN = jwt("admin", TOKEN_KEY);
    private static final String APP_TOKEN = jwt("app", TOKEN_KEY);

    @Container
    private static final PulsarContainer PULSAR = new PulsarContainer("apachepulsar/pulsar:4.2.4")
            .withExposedPorts(PulsarContainer.BROKER_HTTP_PORT, TLS_PORT)
            .withEnv("PULSAR_PREFIX_brokerServicePortTls", String.valueOf(TLS_PORT))
            .withEnv("PULSAR_PREFIX_tlsCertificateFilePath", "/pulsar/tls/server.pem")
            .withEnv("PULSAR_PREFIX_tlsKeyFilePath", "/pulsar/tls/server-key.pem")
            .withEnv("PULSAR_PREFIX_authenticationEnabled", "true")
            .withEnv(
                    "PULSAR_PREFIX_authenticationProviders",
                    "org.apache.pulsar.broker.authentication.AuthenticationProviderToken")
            .withEnv(
                    "PULSAR_PREFIX_tokenSecretKey",
                    "data:;base64," + Base64.getEncoder().encodeToString(TOKEN_KEY))
            .withEnv("PULSAR_PREFIX_superUserRoles", "admin")
            // The broker's own clients, such as the one setting up the standalone namespaces.
            .withEnv(
                    "PULSAR_PREFIX_brokerClientAuthenticationPlugin",
                    "org.apache.pulsar.client.impl.auth.AuthenticationToken")
            .withEnv("PULSAR_PREFIX_brokerClientAuthenticationParameters", "token:" + ADMIN_TOKEN)
            .withCopyToContainer(Transferable.of(TestCertificates.pem("server.pem"), 0644), "/pulsar/tls/server.pem")
            .withCopyToContainer(
                    Transferable.of(TestCertificates.pem("server-key.pem"), 0644), "/pulsar/tls/server-key.pem")
            // The default wait calls the admin API without a token.
            .waitingFor(Wait.forHttp("/admin/v2/namespaces/public")
                    .forPort(PulsarContainer.BROKER_HTTP_PORT)
                    .withHeader("Authorization", "Bearer " + ADMIN_TOKEN)
                    .forResponsePredicate(body -> body.contains("public/default"))
                    .withStartupTimeout(Duration.ofMinutes(3)));

    private static final PulsarProperties PROPERTIES =
            new PulsarProperties(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(3));

    private static HttpClient http;
    private static PulsarClients clients;

    @BeforeAll
    static void connect() {
        http = HttpClient.newHttpClient();
        clients = new PulsarClients(PROPERTIES);
    }

    @AfterAll
    static void disconnect() {
        clients.close();
        http.close();
    }

    @Override
    protected MessageProducer createProducer() {
        return new PulsarMessageProducer(clients, PROPERTIES);
    }

    @Override
    protected MessageReader createReader() {
        return new PulsarMessageReader(clients, PROPERTIES);
    }

    @Override
    protected ConnectionTester createTester() {
        return new PulsarConnectionTester(clients, PROPERTIES);
    }

    @Override
    protected BrokerEndpoint endpoint(Authentication authentication, @Nullable String trustedCertificates) {
        var address = "pulsar+ssl://localhost:" + PULSAR.getMappedPort(TLS_PORT);
        return new BrokerEndpoint(BrokerType.PULSAR, address, authentication, new Tls(false, trustedCertificates));
    }

    @Override
    protected Authentication validAuthentication() {
        return new Authentication.Token(new Secret(APP_TOKEN));
    }

    @Override
    protected Authentication rejectedAuthentication() {
        return new Authentication.Token(new Secret(jwt("app", "another-signing-key-not-the-broker-s".getBytes(UTF_8))));
    }

    @Override
    protected Destination newDestination() {
        var topic = "secured-" + UUID.randomUUID();
        var request = HttpRequest.newBuilder(
                        URI.create(PULSAR.getHttpServiceUrl() + "/admin/v2/persistent/public/default/" + topic))
                .header("Authorization", "Bearer " + ADMIN_TOKEN)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isBetween(200, 299);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return Destination.topic(topic);
    }

    @Test
    void failsTheConnectionTestWithoutAToken() {
        var outcome = tester.test(endpoint(Authentication.none(), TestCertificates.CA));

        assertThat(outcome.succeeded()).isFalse();
    }

    /** A JWT signed with HS256, the way Pulsar's token provider checks it with a secret key. */
    private static String jwt(String subject, byte[] key) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        var header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(UTF_8));
        var payload = encoder.encodeToString(("{\"sub\":\"" + subject + "\"}").getBytes(UTF_8));
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            var signature = mac.doFinal((header + "." + payload).getBytes(UTF_8));
            return header + "." + payload + "." + encoder.encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
