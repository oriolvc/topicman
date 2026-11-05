package com.oriolvc.topicman.connection.infrastructure.crypto;

import com.oriolvc.topicman.shared.infrastructure.EmbeddedDatabase;
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

    /** Fails startup when the master key is missing or malformed, before any credential is written. */
    @Bean
    CredentialCipher credentialCipher(CryptoProperties properties, ObjectProvider<JdbcConnectionDetails> database) {
        return new CredentialCipher(masterKey(properties, database.getIfAvailable()));
    }

    private static SecretKey masterKey(CryptoProperties properties, @Nullable JdbcConnectionDetails database) {
        var masterKey = properties.masterKey();
        var generatedKeyFile = properties.generatedKeyFile();
        boolean keyIsSet = masterKey != null && !masterKey.isBlank();
        boolean embedded = database != null && EmbeddedDatabase.isAt(database.getJdbcUrl());
        if (!keyIsSet && embedded && generatedKeyFile != null) {
            return MasterKeyFile.loadOrGenerate(generatedKeyFile);
        }
        return MasterKey.fromBase64(masterKey);
    }
}
