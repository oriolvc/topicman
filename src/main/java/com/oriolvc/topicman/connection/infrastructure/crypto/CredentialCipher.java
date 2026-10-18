package com.oriolvc.topicman.connection.infrastructure.crypto;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-256-GCM encryption of credentials (ADR 0005). Stored layout:
 * {@code version (1 byte) | IV (12 bytes) | ciphertext + tag (16 bytes)}.
 */
public final class CredentialCipher {

    private static final byte FORMAT_VERSION = 0x01;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int MIN_LENGTH = 1 + IV_LENGTH_BYTES + TAG_LENGTH_BITS / 8;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    CredentialCipher(SecretKey key) {
        this.key = key;
    }

    /** Encrypts with a fresh IV; {@code associatedData} must be given again to decrypt. */
    public byte[] encrypt(byte[] plaintext, byte[] associatedData) {
        var iv = new byte[IV_LENGTH_BYTES];
        random.nextBytes(iv);
        try {
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(associatedData);
            var ciphertext = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(1 + iv.length + ciphertext.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(ciphertext)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] stored, byte[] associatedData) {
        if (stored.length < MIN_LENGTH) {
            throw new CredentialDecryptionException("Stored credentials are truncated");
        }
        if (stored[0] != FORMAT_VERSION) {
            throw new CredentialDecryptionException("Unknown stored credentials format version " + stored[0]);
        }
        try {
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, stored, 1, IV_LENGTH_BYTES));
            cipher.updateAAD(associatedData);
            return cipher.doFinal(stored, 1 + IV_LENGTH_BYTES, stored.length - 1 - IV_LENGTH_BYTES);
        } catch (GeneralSecurityException e) {
            throw new CredentialDecryptionException(
                    "Stored credentials could not be decrypted: wrong master key or tampered data", e);
        }
    }
}
