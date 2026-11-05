package com.oriolvc.topicman.messaging.infrastructure.broker.pulsar;

import com.oriolvc.topicman.messaging.domain.model.Authentication;
import com.oriolvc.topicman.messaging.domain.model.BrokerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One client per endpoint, shared by the producer and the reader. A Pulsar client owns I/O threads
 * and a connection pool, so it is created once and reconnects by itself; producers and readers on
 * top of it are cheap.
 *
 * <p>Clients authenticate with the endpoint's token and, on a {@code pulsar+ssl://} address,
 * verify the broker's certificate and hostname (ADR 0008). The client takes trusted certificates
 * only as a file, so they are written to a temp file, deleted when the client closes; certificates
 * are public, so the file holds no secret.
 */
final class PulsarClients implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(PulsarClients.class);

    private final PulsarProperties properties;
    private final Map<BrokerEndpoint, PulsarClient> clients = new ConcurrentHashMap<>();
    private final Map<PulsarClient, Path> trustFiles = new ConcurrentHashMap<>();

    PulsarClients(PulsarProperties properties) {
        this.properties = properties;
    }

    /**
     * @throws PulsarClientException if the service URL is invalid
     */
    PulsarClient forEndpoint(BrokerEndpoint endpoint) throws PulsarClientException {
        var existing = clients.get(endpoint);
        if (existing != null) {
            return existing;
        }
        var created = create(endpoint);
        var raced = clients.putIfAbsent(endpoint, created);
        if (raced != null) {
            closeQuietly(created);
            return raced;
        }
        return created;
    }

    @Override
    public void close() {
        clients.values().forEach(this::closeQuietly);
        clients.clear();
    }

    private PulsarClient create(BrokerEndpoint endpoint) throws PulsarClientException {
        var operationTimeout = properties.sendTimeout().compareTo(properties.openTimeout()) > 0
                ? properties.sendTimeout()
                : properties.openTimeout();
        var builder = PulsarClient.builder()
                .serviceUrl(endpoint.address())
                .connectionTimeout((int) properties.connectTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .operationTimeout((int) operationTimeout.toMillis(), TimeUnit.MILLISECONDS)
                // Off by default in the Pulsar client; TopicMan always verifies (ADR 0008).
                .enableTlsHostnameVerification(true)
                .allowTlsInsecureConnection(false);
        if (endpoint.authentication() instanceof Authentication.Token token) {
            builder.authentication(AuthenticationFactory.token(token.token().value()));
        }
        var certificates = endpoint.tls().trustedCertificates();
        var trustFile = certificates == null ? null : writeTrustFile(certificates);
        if (trustFile != null) {
            builder.tlsTrustCertsFilePath(trustFile.toString());
        }
        try {
            var client = builder.build();
            if (trustFile != null) {
                trustFiles.put(client, trustFile);
            }
            return client;
        } catch (PulsarClientException | RuntimeException e) {
            deleteQuietly(trustFile);
            throw e;
        }
    }

    private static Path writeTrustFile(String certificates) throws PulsarClientException {
        try {
            var file = Files.createTempFile("topicman-pulsar-trust-", ".pem");
            Files.writeString(file, certificates, StandardCharsets.UTF_8);
            return file;
        } catch (IOException e) {
            throw new PulsarClientException(
                    "Could not write the trusted certificates to a temp file: " + e.getMessage());
        }
    }

    private void closeQuietly(PulsarClient client) {
        try {
            client.close();
        } catch (PulsarClientException e) {
            log.debug("Could not close a Pulsar client cleanly", e);
        } finally {
            deleteQuietly(trustFiles.remove(client));
        }
    }

    private static void deleteQuietly(@Nullable Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.debug("Could not delete the trusted certificates file {}", file, e);
        }
    }
}
