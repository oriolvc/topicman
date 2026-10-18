package com.oriolvc.topicman.connection.infrastructure.crypto;

/** The master key is missing or malformed. Its message never includes the key. */
public final class InvalidMasterKeyException extends IllegalStateException {

    InvalidMasterKeyException(String message) {
        super(message);
    }
}
