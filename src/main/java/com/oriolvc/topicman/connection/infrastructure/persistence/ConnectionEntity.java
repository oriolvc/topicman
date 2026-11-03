package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.shared.domain.BrokerType;
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
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.brokerType = brokerType;
        this.address = address;
        this.credentialsType = credentials.type();
        this.credentialsCiphertext = credentials.ciphertext();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void update(String name, String address, StoredCredentials credentials, Instant updatedAt) {
        this.name = name;
        this.address = address;
        this.credentialsType = credentials.type();
        this.credentialsCiphertext = credentials.ciphertext();
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
        return new StoredCredentials(credentialsType, credentialsCiphertext);
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
