package com.oriolvc.topicman.messaging.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.messaging.domain.exception.InvalidMessageException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeadersTest {

    @Test
    void keepsHeadersInInsertionOrder() {
        var values = new LinkedHashMap<String, String>();
        values.put("trace-id", "abc");
        values.put("content-type", "application/json");

        var headers = Headers.of(values);

        assertThat(headers.values().keySet()).containsExactly("trace-id", "content-type");
    }

    @Test
    void isNotAffectedByChangesToTheOriginalMap() {
        var values = new LinkedHashMap<String, String>();
        values.put("trace-id", "abc");
        var headers = Headers.of(values);

        values.put("tenant", "acme");

        assertThat(headers.values()).doesNotContainKey("tenant");
    }

    @Test
    void cannotBeModified() {
        var headers = Headers.of(Map.of("trace-id", "abc"));

        assertThatThrownBy(() -> headers.values().put("tenant", "acme"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankHeaderName() {
        assertThatThrownBy(() -> Headers.of(Map.of(" ", "abc"))).isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void allowsEmptyHeaderValue() {
        var headers = Headers.of(Map.of("flag", ""));

        assertThat(headers.values()).containsEntry("flag", "");
    }

    @Test
    void noneHasNoHeaders() {
        assertThat(Headers.none().values()).isEmpty();
    }
}
