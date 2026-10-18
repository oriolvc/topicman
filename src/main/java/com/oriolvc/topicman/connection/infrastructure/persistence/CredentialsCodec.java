package com.oriolvc.topicman.connection.infrastructure.persistence;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.Secret;
import com.oriolvc.topicman.connection.infrastructure.crypto.CredentialCipher;
import com.oriolvc.topicman.connection.infrastructure.crypto.CredentialDecryptionException;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converts credentials to and from their stored form: serialized to JSON, then encrypted with the
 * connection id as associated data, so ciphertext copied into another row does not decrypt.
 */
@Component
class CredentialsCodec {

    // A private mapper, so changes to the application's JSON settings never change the stored format.
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final CredentialCipher cipher;

    CredentialsCodec(CredentialCipher cipher) {
        this.cipher = cipher;
    }

    StoredCredentials encode(ConnectionId connectionId, Credentials credentials) {
        return switch (credentials) {
            case Credentials.None none -> new StoredCredentials(CredentialsType.NONE, null);
            case Credentials.Token token ->
                new StoredCredentials(
                        CredentialsType.TOKEN,
                        encrypt(connectionId, new TokenJson(token.token().value())));
            case Credentials.UsernamePassword usernamePassword ->
                new StoredCredentials(
                        CredentialsType.USERNAME_PASSWORD,
                        encrypt(
                                connectionId,
                                new UsernamePasswordJson(
                                        usernamePassword.username(),
                                        usernamePassword.password().value())));
        };
    }

    Credentials decode(ConnectionId connectionId, StoredCredentials stored) {
        if (stored.type() == CredentialsType.NONE) {
            return Credentials.none();
        }
        var ciphertext = stored.ciphertext();
        if (ciphertext == null) {
            throw new IllegalStateException(
                    "Connection " + connectionId + " has " + stored.type() + " credentials but no ciphertext");
        }
        var json = decrypt(connectionId, ciphertext);
        try {
            return switch (stored.type()) {
                case TOKEN ->
                    Credentials.token(
                            new Secret(JSON.readValue(json, TokenJson.class).token()));
                case USERNAME_PASSWORD -> {
                    var value = JSON.readValue(json, UsernamePasswordJson.class);
                    yield Credentials.usernamePassword(value.username(), new Secret(value.password()));
                }
                case NONE -> Credentials.none();
            };
        } catch (JacksonException e) {
            // Never chain the cause: Jackson's message can quote the decrypted JSON.
            throw new IllegalStateException(
                    "Decrypted credentials of connection " + connectionId + " are not valid " + stored.type());
        }
    }

    private byte[] encrypt(ConnectionId connectionId, Object json) {
        return cipher.encrypt(JSON.writeValueAsBytes(json), associatedData(connectionId));
    }

    private byte[] decrypt(ConnectionId connectionId, byte[] ciphertext) {
        try {
            return cipher.decrypt(ciphertext, associatedData(connectionId));
        } catch (CredentialDecryptionException e) {
            throw new CredentialDecryptionException(
                    "Credentials of connection " + connectionId + " could not be decrypted", e);
        }
    }

    private static byte[] associatedData(ConnectionId connectionId) {
        return connectionId.value().toString().getBytes(UTF_8);
    }

    private record TokenJson(String token) {}

    private record UsernamePasswordJson(String username, String password) {}
}
