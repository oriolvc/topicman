package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Collection, template and environment ids wrap positive sequence values (ADR 0007). */
class IdsTest {

    @Test
    void wrapPositiveValues() {
        assertThat(new CollectionId(1).value()).isEqualTo(1);
        assertThat(new TemplateId(2)).hasToString("2");
        assertThat(new EnvironmentId(3).value()).isEqualTo(3);
    }

    @Test
    void rejectZeroAndNegativeValues() {
        assertThatThrownBy(() -> new CollectionId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TemplateId(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EnvironmentId(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
