package com.oriolvc.topicman.connection.infrastructure.crypto;

import javax.crypto.SecretKey;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfiguration {

    private static final String EMBEDDED_DATABASE_URL = "jdbc:h2:";

    /** Fails startup when the master key is missing or malformed, before any credential is written. */
    @Bean
    CredentialCipher credentialCipher(CryptoProperties properties, ObjectProvider<JdbcConnectionDetails> database) {
        return new CredentialCipher(masterKey(properties, database.getIfAvailable()));
    }

    /** Asks the connection, not the URL property: Docker Compose and Testcontainers replace the latter. */
    private static SecretKey masterKey(CryptoProperties properties, @Nullable JdbcConnectionDetails database) {
        var masterKey = properties.masterKey();
        var generatedKeyFile = properties.generatedKeyFile();
        boolean keyIsSet = masterKey != null && !masterKey.isBlank();
        boolean embedded = database != null && database.getJdbcUrl().startsWith(EMBEDDED_DATABASE_URL);
        if (!keyIsSet && embedded && generatedKeyFile != null) {
            return MasterKeyFile.loadOrGenerate(generatedKeyFile);
        }
        return MasterKey.fromBase64(masterKey);
    }
}
