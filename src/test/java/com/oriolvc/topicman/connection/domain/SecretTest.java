package com.oriolvc.topicman.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecretTest {

    @Test
    void keepsItsValue() {
        var secret = new Secret("s3cr3t");

        assertThat(secret.value()).isEqualTo("s3cr3t");
    }

    @Test
    void neverShowsItsValueInToString() {
        var secret = new Secret("s3cr3t");

        assertThat(secret.toString()).isEqualTo("****");
    }

    @Test
    void rejectsEmptySecret() {
        assertThatThrownBy(() -> new Secret("")).isInstanceOf(InvalidConnectionException.class);
    }
}
