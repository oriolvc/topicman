package com.oriolvc.topicman.connection.infrastructure.crypto;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfiguration {

    /** Fails startup when the master key is missing or malformed, before any credential is written. */
    @Bean
    CredentialCipher credentialCipher(CryptoProperties properties) {
        return new CredentialCipher(MasterKey.fromBase64(properties.masterKey()));
    }
}
