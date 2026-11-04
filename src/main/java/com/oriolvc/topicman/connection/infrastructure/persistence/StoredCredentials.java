package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.shared.domain.SaslMechanism;
import org.jspecify.annotations.Nullable;

/**
 * Credentials as stored: a plain type, the Kafka SASL mechanism, which is not a secret (ADR 0008),
 * and, unless the type is NONE, the encrypted JSON.
 */
record StoredCredentials(CredentialsType type, @Nullable SaslMechanism saslMechanism, byte @Nullable [] ciphertext) {}
