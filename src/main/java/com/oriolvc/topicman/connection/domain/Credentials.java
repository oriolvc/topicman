package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.Objects;

/**
 * How TopicMan authenticates to a broker. SASL mechanisms and TLS settings are added in Phase 5.
 */
public sealed interface Credentials {

    boolean isSupportedBy(BrokerType brokerType);

    static Credentials none() {
        return None.INSTANCE;
    }

    static Credentials token(Secret token) {
        return new Token(token);
    }

    static Credentials usernamePassword(String username, Secret password) {
        return new UsernamePassword(username, password);
    }

    /** No authentication. */
    enum None implements Credentials {
        INSTANCE;

        @Override
        public boolean isSupportedBy(BrokerType brokerType) {
            return true;
        }
    }

    /** A bearer token, such as a Pulsar JWT. */
    record Token(Secret token) implements Credentials {

        public Token {
            Objects.requireNonNull(token, "token");
        }

        @Override
        public boolean isSupportedBy(BrokerType brokerType) {
            return brokerType == BrokerType.PULSAR;
        }
    }

    /** A username and password, such as Kafka SASL or RabbitMQ user credentials. */
    record UsernamePassword(String username, Secret password) implements Credentials {

        public UsernamePassword {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
            username = username.strip();
            if (username.isEmpty()) {
                throw new InvalidConnectionException("Username must not be blank");
            }
        }

        @Override
        public boolean isSupportedBy(BrokerType brokerType) {
            return brokerType == BrokerType.KAFKA || brokerType == BrokerType.RABBITMQ;
        }
    }
}
