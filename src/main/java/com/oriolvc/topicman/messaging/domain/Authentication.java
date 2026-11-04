package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * How an adapter authenticates to a broker (ADR 0008). Mirrors the connection context's
 * credentials without depending on them; the connection context has already checked that the
 * broker supports them. Secrets stay wrapped in {@link Secret}, whose {@code toString()} is masked.
 */
public sealed interface Authentication {

    static Authentication none() {
        return None.INSTANCE;
    }

    enum None implements Authentication {
        INSTANCE
    }

    /** A bearer token: a Pulsar JWT. */
    record Token(Secret token) implements Authentication {

        public Token {
            Objects.requireNonNull(token, "token");
        }
    }

    /** @param mechanism the Kafka SASL mechanism; null for RabbitMQ */
    record UsernamePassword(
            String username, Secret password, @Nullable SaslMechanism mechanism) implements Authentication {

        public UsernamePassword {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }
    }
}
