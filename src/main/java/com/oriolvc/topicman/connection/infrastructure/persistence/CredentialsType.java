package com.oriolvc.topicman.connection.infrastructure.persistence;

/** Stored in plain text next to the ciphertext, so connections can be listed without decrypting. */
enum CredentialsType {
    NONE,
    TOKEN,
    USERNAME_PASSWORD
}
