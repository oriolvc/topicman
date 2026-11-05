package com.oriolvc.topicman.connection.domain.model;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Objects;

/**
 * CA certificates in PEM form that a connection trusts instead of the JVM's default CAs, for
 * brokers whose certificate a private or self-signed CA issued (ADR 0008). Certificates are
 * public: they are stored in plain text and returned by the API. They are parsed here, with the
 * JDK, so a malformed certificate is rejected when the connection is saved.
 */
public record TrustedCertificates(String pem) {

    public static final int MAX_LENGTH = 65_536;

    private static final String PEM_CERTIFICATE = "-----BEGIN CERTIFICATE-----";

    public TrustedCertificates {
        Objects.requireNonNull(pem, "pem");
        pem = pem.strip();
        if (pem.isEmpty()) {
            throw new InvalidConnectionException("Trusted certificates must not be blank");
        }
        if (pem.length() > MAX_LENGTH) {
            throw new InvalidConnectionException("Trusted certificates must be at most " + MAX_LENGTH + " characters");
        }
        // Checked before parsing: a pasted key would be stored in plain text and returned.
        if (pem.contains("PRIVATE KEY-----")) {
            throw new InvalidConnectionException(
                    "Trusted certificates must not contain a private key; paste only the CA certificates");
        }
        if (!pem.contains(PEM_CERTIFICATE)) {
            throw new InvalidConnectionException(
                    "Trusted certificates must be PEM certificates, each starting with " + PEM_CERTIFICATE);
        }
        count(pem);
    }

    /** How many certificates the PEM holds. */
    public int count() {
        return count(pem);
    }

    private static int count(String pem) {
        try {
            var certificates = CertificateFactory.getInstance("X.509")
                    .generateCertificates(new ByteArrayInputStream(pem.getBytes(UTF_8)));
            if (certificates.isEmpty()) {
                throw new InvalidConnectionException("Trusted certificates must hold at least one PEM certificate");
            }
            return certificates.size();
        } catch (CertificateException e) {
            // The JDK's message never quotes the input, but it says nothing useful either.
            throw new InvalidConnectionException("Trusted certificates are not valid PEM certificates");
        }
    }
}
