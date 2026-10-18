package com.oriolvc.topicman.connection.infrastructure.persistence;

import org.jspecify.annotations.Nullable;

/** Credentials as stored: a plain type and, unless the type is NONE, the encrypted JSON. */
record StoredCredentials(CredentialsType type, byte @Nullable [] ciphertext) {}
