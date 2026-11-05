package com.oriolvc.topicman.connection.infrastructure.persistence;

import com.oriolvc.topicman.connection.domain.model.TlsSettings;
import com.oriolvc.topicman.connection.domain.model.TrustedCertificates;
import org.jspecify.annotations.Nullable;

/** TLS settings as stored, in plain text: none of them is a secret (ADR 0008). */
record StoredTls(boolean enabled, @Nullable String trustedCertificates) {

    static StoredTls of(TlsSettings tls) {
        var certificates = tls.trustedCertificates();
        return new StoredTls(tls.enabled(), certificates == null ? null : certificates.pem());
    }

    TlsSettings toDomain() {
        return new TlsSettings(
                enabled, trustedCertificates == null ? null : new TrustedCertificates(trustedCertificates));
    }
}
