package com.oriolvc.topicman.messaging.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SendReceiptTest {

    @Test
    void rejectsBlankReference() {
        assertThatThrownBy(() -> new SendReceipt(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
