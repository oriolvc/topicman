package com.oriolvc.topicman.connection.infrastructure.crypto;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/** Parses the AES-256 master key from its Base64 form in {@code TOPICMAN_MASTER_KEY}. */
final class MasterKey {

    private static final int KEY_LENGTH_BYTES = 32;
    private static final String HOW_TO_GENERATE = "Generate one with: openssl rand -base64 32";

    private MasterKey() {}

    static SecretKey fromBase64(@Nullable String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new InvalidMasterKeyException("TOPICMAN_MASTER_KEY is not set. " + HOW_TO_GENERATE);
        }
        byte[] key;
        try {
            key = Base64.getDecoder().decode(encoded.strip());
        } catch (IllegalArgumentException e) {
            // The cause is dropped on purpose: its message can quote part of the key.
            throw new InvalidMasterKeyException("TOPICMAN_MASTER_KEY is not valid Base64. " + HOW_TO_GENERATE);
        }
        if (key.length != KEY_LENGTH_BYTES) {
            throw new InvalidMasterKeyException("TOPICMAN_MASTER_KEY must decode to " + KEY_LENGTH_BYTES
                    + " bytes, but has " + key.length + ". " + HOW_TO_GENERATE);
        }
        return new SecretKeySpec(key, "AES");
    }
}
