package com.oriolvc.topicman.messaging.infrastructure.broker.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ShutdownSignalException;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Reads the broker's reason out of the client's exceptions, which wrap it in protocol details. */
final class AmqpErrors {

    private static final int NOT_FOUND = 404;

    private AmqpErrors() {}

    /** The channel or connection close that caused the failure, if the broker closed one. */
    static Optional<ShutdownSignalException> shutdownCause(@Nullable Throwable failure) {
        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ShutdownSignalException shutdown) {
                return Optional.of(shutdown);
            }
        }
        return Optional.empty();
    }

    static boolean isNotFound(Throwable failure) {
        return shutdownCause(failure)
                .map(ShutdownSignalException::getReason)
                .filter(reason -> reason instanceof AMQP.Channel.Close close && close.getReplyCode() == NOT_FOUND)
                .isPresent();
    }

    /** The broker's reply text, such as {@code NOT_FOUND - no exchange 'orders'}, or the message. */
    static String describe(Throwable failure) {
        var reason =
                shutdownCause(failure).map(ShutdownSignalException::getReason).orElse(null);
        if (reason instanceof AMQP.Channel.Close close) {
            return close.getReplyText();
        }
        if (reason instanceof AMQP.Connection.Close close) {
            return close.getReplyText();
        }
        return String.valueOf(failure.getMessage());
    }
}
