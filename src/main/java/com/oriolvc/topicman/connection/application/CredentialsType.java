package com.oriolvc.topicman.connection.application;

import com.oriolvc.topicman.connection.domain.Credentials;

/** Which kind of credentials a connection has. Results expose this instead of the secrets. */
public enum CredentialsType {
    NONE,
    TOKEN,
    USERNAME_PASSWORD;

    static CredentialsType of(Credentials credentials) {
        return switch (credentials) {
            case Credentials.None none -> NONE;
            case Credentials.Token token -> TOKEN;
            case Credentials.UsernamePassword usernamePassword -> USERNAME_PASSWORD;
        };
    }
}
