package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "broker_connection")
class ConnectionEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "credentials_type", nullable = false)
    private CredentialsType credentialsType;

    @Column(name = "credentials_ciphertext")
    private byte @Nullable [] credentialsCiphertext;

    @Enumerated(EnumType.STRING)
    @Column(name = "sasl_mechanism")
    private @Nullable SaslMechanism saslMechanism;

    @Column(name = "tls_enabled", nullable = false)
    private boolean tlsEnabled;

    @Column(name = "tls_trusted_certificates")
    private @Nullable String tlsTrustedCertificates;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Null until the first insert, which is how Spring Data tells a new entity from an existing one.
    @Version
    private @Nullable Long version;

    protected ConnectionEntity() {}

    ConnectionEntity(
            long id,
            String name,
            BrokerType brokerType,
            String address,
            StoredCredentials credentials,
            StoredTls tls,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.brokerType = brokerType;
        this.createdAt = createdAt;
        update(name, address, credentials, tls, updatedAt);
    }

    void update(String name, String address, StoredCredentials credentials, StoredTls tls, Instant updatedAt) {
        this.name = name;
        this.address = address;
        this.credentialsType = credentials.type();
        this.saslMechanism = credentials.saslMechanism();
        this.credentialsCiphertext = credentials.ciphertext();
        this.tlsEnabled = tls.enabled();
        this.tlsTrustedCertificates = tls.trustedCertificates();
        this.updatedAt = updatedAt;
    }

    long id() {
        return id;
    }

    String name() {
        return name;
    }

    BrokerType brokerType() {
        return brokerType;
    }

    String address() {
        return address;
    }

    StoredCredentials credentials() {
        return new StoredCredentials(credentialsType, saslMechanism, credentialsCiphertext);
    }

    StoredTls tls() {
        return new StoredTls(tlsEnabled, tlsTrustedCertificates);
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
