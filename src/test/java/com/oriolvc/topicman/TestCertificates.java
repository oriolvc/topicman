package com.oriolvc.topicman;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The test-only certificates in {@code src/test/resources/tls/}, made by its {@code generate.sh}
 * (ADR 0008). Their private keys are public: never use them outside tests.
 */
public final class TestCertificates {

    /** The test CA, which issued {@code server.pem} for localhost and 127.0.0.1. */
    public static final String CA = pem("ca.pem");

    /** A CA that issued nothing the test brokers use, to test an untrusted certificate. */
    public static final String OTHER_CA = pem("other-ca.pem");

    private TestCertificates() {}

    public static String pem(String fileName) {
        try (var in = TestCertificates.class.getResourceAsStream("/tls/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("No test certificate " + fileName);
            }
            return new String(in.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A test certificate file on disk, to mount into a broker container. */
    public static Path file(String fileName) {
        var url = TestCertificates.class.getResource("/tls/" + fileName);
        if (url == null) {
            throw new IllegalStateException("No test certificate " + fileName);
        }
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
