package com.oriolvc.topicman.connection.infrastructure.crypto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The master key generated for the embedded database when none is set (ADR 0010). The file holds the
 * same Base64 form as {@code TOPICMAN_MASTER_KEY}, so it can become that variable's value later.
 */
final class MasterKeyFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterKeyFile.class);
    private static final int KEY_LENGTH_BYTES = 32;

    private MasterKeyFile() {}

    static SecretKey loadOrGenerate(Path file) {
        try {
            if (Files.notExists(file)) {
                generate(file.toAbsolutePath());
            }
            var key = load(file);
            LOGGER.warn(
                    "Using the generated master key in {}. Anyone with a copy of that directory can decrypt the saved"
                            + " credentials: set TOPICMAN_MASTER_KEY to use real credentials.",
                    file);
            return key;
        } catch (IOException e) {
            // Only the type of the cause: its message has no reason to reach the key, but needs no chance to.
            throw new InvalidMasterKeyException("Could not use the generated master key file " + file + ": "
                    + e.getClass().getSimpleName());
        }
    }

    private static SecretKey load(Path file) throws IOException {
        try {
            return MasterKey.fromBase64(Files.readString(file));
        } catch (InvalidMasterKeyException e) {
            throw new InvalidMasterKeyException("The generated master key file " + file
                    + " is not valid. Restore it, or delete it to generate a new one and enter the saved credentials"
                    + " again.");
        }
    }

    /** Written to a temporary file first, which only its owner can read, so no partial key is ever left. */
    private static void generate(Path file) throws IOException {
        var directory = Files.createDirectories(file.getParent());
        var bytes = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(bytes);
        var temporary = Files.createTempFile(directory, "master-key", ".tmp");
        try {
            Files.writeString(temporary, Base64.getEncoder().encodeToString(bytes));
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
