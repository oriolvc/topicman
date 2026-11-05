package com.oriolvc.topicman.connection.infrastructure.crypto;

import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code topicman.crypto.*}; {@code master-key} defaults to {@code TOPICMAN_MASTER_KEY}.
 * The key is validated when the cipher is created, not while binding, because Spring Boot's
 * binding failure report prints the offending property value. {@code generated-key-file} is where
 * the embedded database keeps a generated key when none is set (ADR 0010).
 */
@ConfigurationProperties("topicman.crypto")
public record CryptoProperties(
        @Nullable String masterKey, @Nullable Path generatedKeyFile) {

    @Override
    public String toString() {
        return "CryptoProperties[masterKey=****, generatedKeyFile=" + generatedKeyFile + "]";
    }
}
