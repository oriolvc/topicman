package com.oriolvc.topicman.connection.infrastructure.crypto;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bound from {@code topicman.crypto.*}; {@code master-key} defaults to {@code TOPICMAN_MASTER_KEY}.
 * The key is validated when the cipher is created, not while binding, because Spring Boot's
 * binding failure report prints the offending property value.
 */
@ConfigurationProperties("topicman.crypto")
public record CryptoProperties(@Nullable String masterKey) {

    @Override
    public String toString() {
        return "CryptoProperties[masterKey=****]";
    }
}
