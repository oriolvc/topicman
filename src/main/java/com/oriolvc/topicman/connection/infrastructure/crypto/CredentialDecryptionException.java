package com.oriolvc.topicman.connection.infrastructure.crypto;

/**
 * Stored credentials could not be decrypted: a different master key, tampered data or an unknown
 * format. Its message never includes key or credential material.
 */
public final class CredentialDecryptionException extends RuntimeException {

    public CredentialDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    CredentialDecryptionException(String message) {
        super(message);
    }
}
