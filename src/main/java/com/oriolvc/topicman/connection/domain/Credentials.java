package com.oriolvc.topicman.connection.domain;

import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import com.oriolvc.topicman.shared.domain.Secret;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** How TopicMan authenticates to a broker (ADR 0005, ADR 0008). */
public sealed interface Credentials {

    /**
     * These credentials as the broker uses them, with its defaults filled in.
     *
     * @throws InvalidConnectionException if the broker does not support them
     */
    Credentials forBroker(BrokerType brokerType);

    static Credentials none() {
        return None.INSTANCE;
    }

    static Credentials token(Secret token) {
        return new Token(token);
    }

    /** A username and password with no SASL mechanism: Kafka defaults it to {@code PLAIN}. */
    static Credentials usernamePassword(String username, Secret password) {
        return new UsernamePassword(username, password, null);
    }

    static Credentials usernamePassword(String username, Secret password, SaslMechanism mechanism) {
        return new UsernamePassword(username, password, Objects.requireNonNull(mechanism, "mechanism"));
    }

    /** No authentication. */
    enum None implements Credentials {
        INSTANCE;

        @Override
        public Credentials forBroker(BrokerType brokerType) {
            return this;
        }
    }

    /** A bearer token, such as a Pulsar JWT. */
    record Token(Secret token) implements Credentials {

        public Token {
            Objects.requireNonNull(token, "token");
        }

        @Override
        public Credentials forBroker(BrokerType brokerType) {
            if (brokerType != BrokerType.PULSAR) {
                throw unsupported("Token", brokerType);
            }
            return this;
        }
    }

    /**
     * A username and password: Kafka SASL, with its mechanism, or RabbitMQ user credentials, which
     * have none.
     */
    record UsernamePassword(
            String username, Secret password, @Nullable SaslMechanism mechanism) implements Credentials {

        public UsernamePassword {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
            username = username.strip();
            if (username.isEmpty()) {
                throw new InvalidConnectionException("Username must not be blank");
            }
        }

        @Override
        public Credentials forBroker(BrokerType brokerType) {
            return switch (brokerType) {
                case KAFKA -> mechanism == null ? new UsernamePassword(username, password, SaslMechanism.PLAIN) : this;
                case RABBITMQ -> {
                    if (mechanism != null) {
                        throw new InvalidConnectionException("A SASL mechanism is only supported by KAFKA");
                    }
                    yield this;
                }
                case PULSAR -> throw unsupported("UsernamePassword", brokerType);
            };
        }
    }

    private static InvalidConnectionException unsupported(String credentialsType, BrokerType brokerType) {
        return new InvalidConnectionException(credentialsType + " credentials are not supported by " + brokerType);
    }
}
