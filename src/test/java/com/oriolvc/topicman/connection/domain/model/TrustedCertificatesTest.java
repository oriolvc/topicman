package com.oriolvc.topicman.connection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.connection.domain.exception.InvalidConnectionException;
import org.junit.jupiter.api.Test;

class TrustedCertificatesTest {

    @Test
    void keepsAPemCertificateWithoutSurroundingWhitespace() {
        var certificates = new TrustedCertificates("\n" + Pem.CA + "\n\n");

        assertThat(certificates.pem()).isEqualTo(Pem.CA.strip());
        assertThat(certificates.count()).isEqualTo(1);
    }

    @Test
    void acceptsSeveralCertificates() {
        var certificates = new TrustedCertificates(Pem.CA + Pem.OTHER_CA);

        assertThat(certificates.count()).isEqualTo(2);
    }

    @Test
    void rejectsTextThatIsNotACertificate() {
        assertThatThrownBy(() -> new TrustedCertificates("not a certificate"))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining("PEM");
    }

    @Test
    void rejectsACertificateWhoseContentIsBroken() {
        var broken = Pem.CA.replaceFirst("(?m)^MII", "MIX");

        assertThatThrownBy(() -> new TrustedCertificates(broken)).isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void rejectsAPrivateKey() {
        // Trusted certificates are stored in plain text and returned by the API, so a pasted key
        // would leak.
        assertThatThrownBy(() -> new TrustedCertificates(Pem.CA + Pem.PRIVATE_KEY))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining("private key")
                .hasMessageNotContaining("MII");
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> new TrustedCertificates(" \n ")).isInstanceOf(InvalidConnectionException.class);
    }

    @Test
    void rejectsTextLongerThanTheLimit() {
        var tooLong = Pem.CA + "x".repeat(TrustedCertificates.MAX_LENGTH);

        assertThatThrownBy(() -> new TrustedCertificates(tooLong))
                .isInstanceOf(InvalidConnectionException.class)
                .hasMessageContaining(String.valueOf(TrustedCertificates.MAX_LENGTH));
    }
}
