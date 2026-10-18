package com.oriolvc.topicman.connection.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;

class MasterKeyTest {

    @Test
    void parsesA256BitBase64Key() {
        var encoded = Base64.getEncoder().encodeToString(new byte[32]);

        var key = MasterKey.fromBase64(encoded);

        assertThat(key.getAlgorithm()).isEqualTo("AES");
        assertThat(key.getEncoded()).hasSize(32);
    }

    @Test
    void rejectsMissingKey() {
        assertThatThrownBy(() -> MasterKey.fromBase64(null))
                .isInstanceOf(InvalidMasterKeyException.class)
                .hasMessageContaining("TOPICMAN_MASTER_KEY");
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> MasterKey.fromBase64("  ")).isInstanceOf(InvalidMasterKeyException.class);
    }

    @Test
    void rejectsKeyThatIsNotBase64WithoutShowingIt() {
        assertThatThrownBy(() -> MasterKey.fromBase64("not-base64-%%%"))
                .isInstanceOf(InvalidMasterKeyException.class)
                .hasMessageNotContaining("not-base64-%%%")
                .hasNoCause();
    }

    @Test
    void rejectsKeyOfTheWrongLengthWithoutShowingIt() {
        var encoded = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> MasterKey.fromBase64(encoded))
                .isInstanceOf(InvalidMasterKeyException.class)
                .hasMessageNotContaining(encoded);
    }
}
