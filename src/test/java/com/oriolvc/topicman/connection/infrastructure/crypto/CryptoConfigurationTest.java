package com.oriolvc.topicman.connection.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CryptoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(CryptoConfiguration.class);

    @Test
    void createsTheCipherFromAValidMasterKey() {
        contextRunner
                .withPropertyValues("topicman.crypto.master-key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=")
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
}
