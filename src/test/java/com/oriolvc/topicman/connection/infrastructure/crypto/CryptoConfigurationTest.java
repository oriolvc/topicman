package com.oriolvc.topicman.connection.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CryptoConfigurationTest {

    private static final String VALID_KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(CryptoConfiguration.class);

    @TempDir
    private Path directory;

    private ApplicationContextRunner usingDatabase(String jdbcUrl) {
        return contextRunner
                .withBean(JdbcConnectionDetails.class, () -> new Details(jdbcUrl))
                .withPropertyValues("topicman.crypto.generated-key-file=" + keyFile());
    }

    private Path keyFile() {
        return directory.resolve("master.key");
    }

    @Test
    void createsTheCipherFromAValidMasterKey() {
        contextRunner
                .withPropertyValues("topicman.crypto.master-key=" + VALID_KEY)
                .run(context -> assertThat(context).hasSingleBean(CredentialCipher.class));
    }

    @Test
    void failsStartupWithoutAMasterKey() {
        contextRunner.run(context ->
                assertThat(context).hasFailed().getFailure().rootCause().isInstanceOf(InvalidMasterKeyException.class));
    }

    @Test
    void failsStartupWithAMalformedMasterKeyWithoutShowingIt() {
        contextRunner
                .withPropertyValues("topicman.crypto.master-key=dG9vLXNob3J0LWtleQ==")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .satisfies(failure -> assertThat(failure.toString()).doesNotContain("dG9vLXNob3J0LWtleQ=="))
                        .rootCause()
                        .isInstanceOf(InvalidMasterKeyException.class));
    }

    @Test
    void generatesAMissingKeyOnTheEmbeddedDatabase() {
        usingDatabase("jdbc:h2:file:/data/topicman").run(context -> {
            assertThat(context).hasSingleBean(CredentialCipher.class);
            assertThat(keyFile()).exists();
        });
    }

    @Test
    void failsStartupWithoutAKeyOnPostgresqlAndGeneratesNone() {
        usingDatabase("jdbc:postgresql://localhost/topicman").run(context -> {
            assertThat(context).hasFailed().getFailure().rootCause().isInstanceOf(InvalidMasterKeyException.class);
            assertThat(keyFile()).doesNotExist();
        });
    }

    @Test
    void prefersTheKeyThatIsSetOnTheEmbeddedDatabase() {
        usingDatabase("jdbc:h2:file:/data/topicman")
                .withPropertyValues("topicman.crypto.master-key=" + VALID_KEY)
                .run(context -> {
                    assertThat(context).hasSingleBean(CredentialCipher.class);
                    assertThat(keyFile()).doesNotExist();
                });
    }

    @Test
    void rejectsAMalformedKeyOnTheEmbeddedDatabaseInsteadOfGeneratingOne() {
        usingDatabase("jdbc:h2:file:/data/topicman")
                .withPropertyValues("topicman.crypto.master-key=dG9vLXNob3J0LWtleQ==")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(keyFile()).doesNotExist();
                });
    }

    private record Details(String jdbcUrl) implements JdbcConnectionDetails {

        @Override
        public String getJdbcUrl() {
            return jdbcUrl;
        }

        @Override
        public String getUsername() {
            return "user";
        }

        @Override
        public String getPassword() {
            return "password";
        }
    }
}
