package com.oriolvc.topicman.connection.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MasterKeyFileTest {

    @TempDir
    private Path directory;

    @Test
    void generatesAKeyInANewDirectory() {
        var file = directory.resolve("data/master.key");

        var key = MasterKeyFile.loadOrGenerate(file);

        assertThat(file).exists();
        assertThat(key.getEncoded()).hasSize(32);
        assertThat(key.getAlgorithm()).isEqualTo("AES");
    }

    @Test
    void reusesTheGeneratedKey() {
        var file = directory.resolve("master.key");

        var first = MasterKeyFile.loadOrGenerate(file);
        var second = MasterKeyFile.loadOrGenerate(file);

        assertThat(second.getEncoded()).isEqualTo(first.getEncoded());
    }

    @Test
    void generatesADifferentKeyEachTime() {
        var first = MasterKeyFile.loadOrGenerate(directory.resolve("first.key"));
        var second = MasterKeyFile.loadOrGenerate(directory.resolve("second.key"));

        assertThat(second.getEncoded()).isNotEqualTo(first.getEncoded());
    }

    @Test
    void onlyItsOwnerCanReadTheFile() throws IOException {
        var file = directory.resolve("master.key");

        MasterKeyFile.loadOrGenerate(file);

        assertThat(Files.getPosixFilePermissions(file))
                .isEqualTo(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        var file = directory.resolve("master.key");

        MasterKeyFile.loadOrGenerate(file);

        try (var files = Files.list(directory)) {
            assertThat(files).containsExactly(file);
        }
    }

    @Test
    void writesAKeyThatCanBeUsedAsTheEnvironmentVariable() throws IOException {
        var file = directory.resolve("master.key");

        var key = MasterKeyFile.loadOrGenerate(file);

        assertThat(MasterKey.fromBase64(Files.readString(file)).getEncoded()).isEqualTo(key.getEncoded());
    }

    @Test
    void rejectsAnInvalidFileWithoutShowingItsContentOrReplacingIt() throws IOException {
        var file = directory.resolve("master.key");
        Files.writeString(file, "not-a-valid-key");

        assertThatThrownBy(() -> MasterKeyFile.loadOrGenerate(file))
                .isInstanceOf(InvalidMasterKeyException.class)
                .hasMessageContaining(file.toString())
                .hasMessageNotContaining("not-a-valid-key");

        assertThat(file).hasContent("not-a-valid-key");
    }

    @Test
    void reportsADirectoryItCannotUse() throws IOException {
        var notADirectory = Files.writeString(directory.resolve("file"), "");

        assertThatThrownBy(() -> MasterKeyFile.loadOrGenerate(notADirectory.resolve("master.key")))
                .isInstanceOf(InvalidMasterKeyException.class)
                .hasMessageContaining("master.key");
    }
}
