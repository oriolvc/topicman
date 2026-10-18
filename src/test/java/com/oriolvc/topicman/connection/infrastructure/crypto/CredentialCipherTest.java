package com.oriolvc.topicman.connection.infrastructure.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class CredentialCipherTest {

    private static final byte[] PLAINTEXT = "{\"password\":\"s3cr3t\"}".getBytes(UTF_8);
    private static final byte[] AAD = "connection-1".getBytes(UTF_8);

    private final CredentialCipher cipher = aCipherWithKeyByte(1);

    @Test
    void decryptsWhatItEncrypts() {
        var stored = cipher.encrypt(PLAINTEXT, AAD);

        assertThat(cipher.decrypt(stored, AAD)).isEqualTo(PLAINTEXT);
    }

    @Test
    void storesVersionIvAndCiphertextWithTag() {
        var stored = cipher.encrypt(PLAINTEXT, AAD);

        assertThat(stored[0]).isEqualTo((byte) 0x01);
        assertThat(stored).hasSize(1 + 12 + PLAINTEXT.length + 16);
        assertThat(new String(stored, UTF_8)).doesNotContain("s3cr3t");
    }

    @Test
    void usesAFreshIvForEachEncryption() {
        var first = cipher.encrypt(PLAINTEXT, AAD);
        var second = cipher.encrypt(PLAINTEXT, AAD);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void rejectsCiphertextBoundToOtherData() {
        var stored = cipher.encrypt(PLAINTEXT, AAD);

        assertThatThrownBy(() -> cipher.decrypt(stored, "connection-2".getBytes(UTF_8)))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsTamperedCiphertext() {
        var stored = cipher.encrypt(PLAINTEXT, AAD);
        stored[stored.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(stored, AAD)).isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsCiphertextEncryptedWithAnotherKey() {
        var stored = aCipherWithKeyByte(2).encrypt(PLAINTEXT, AAD);

        assertThatThrownBy(() -> cipher.decrypt(stored, AAD)).isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsUnknownFormatVersion() {
        var stored = cipher.encrypt(PLAINTEXT, AAD);
        stored[0] = 0x02;

        assertThatThrownBy(() -> cipher.decrypt(stored, AAD)).isInstanceOf(CredentialDecryptionException.class);
    }

    @Test
    void rejectsTruncatedCiphertext() {
        assertThatThrownBy(() -> cipher.decrypt(new byte[] {0x01, 0x02}, AAD))
                .isInstanceOf(CredentialDecryptionException.class);
    }

    private static CredentialCipher aCipherWithKeyByte(int value) {
        var key = new byte[32];
        Arrays.fill(key, (byte) value);
        return new CredentialCipher(MasterKey.fromBase64(Base64.getEncoder().encodeToString(key)));
    }
}
