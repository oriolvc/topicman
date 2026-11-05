package com.oriolvc.topicman.connection.application.model;

import com.oriolvc.topicman.connection.application.usecase.GetBrokerAccessUseCase;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Decrypted credentials, as {@link GetBrokerAccessUseCase} hands them to the messaging context
 * (ADR 0008). Built from shared kernel types only, so other contexts never touch the connection
 * domain. Secrets stay wrapped in {@link Secret}, whose {@code toString()} is masked.
 */
public sealed interface BrokerCredentials {

    public static BrokerCredentials none() {
        return None.INSTANCE;
    }

    enum None implements BrokerCredentials {
        INSTANCE
    }

    record Token(Secret token) implements BrokerCredentials {

        public Token {
            Objects.requireNonNull(token, "token");
        }
    }

    /** @param mechanism the Kafka SASL mechanism; null for brokers without one */
    record UsernamePassword(
            String username, Secret password, @Nullable SaslMechanism mechanism) implements BrokerCredentials {

        public UsernamePassword {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }
    }
}
