package com.oriolvc.topicman.messaging.domain;

import com.oriolvc.topicman.shared.domain.DomainException;
import org.jspecify.annotations.Nullable;

/**
 * The broker rejected a message or did not acknowledge it in time. Adapters throw it instead of
 * their client's exceptions, so no broker client type leaves the adapter. When several copies were
 * requested, {@link #sent()} says how many were acknowledged before the failure.
 */
public final class ProduceFailedException extends DomainException {

    public static final String CODE = "messaging.produce-failed";

    private final int sent;

    public ProduceFailedException(String message) {
        this(message, null, 0);
    }

    public ProduceFailedException(String message, Throwable cause) {
        this(message, cause, 0);
    }

    private ProduceFailedException(String message, @Nullable Throwable cause, int sent) {
        super(CODE, message, cause);
        this.sent = sent;
    }

    /** The same failure, after {@code sent} copies were acknowledged. */
    public static ProduceFailedException afterSending(int sent, ProduceFailedException failure) {
        return new ProduceFailedException(String.valueOf(failure.getMessage()), failure, sent);
    }

    public int sent() {
        return sent;
    }
}
