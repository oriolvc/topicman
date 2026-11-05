package com.oriolvc.topicman.collection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.exception.InvalidCollectionException;
import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import org.junit.jupiter.api.Test;

/** The names of collections, templates and environments share one rule set. */
class NamesTest {

    @Test
    void stripsSurroundingWhitespace() {
        assertThat(new CollectionName("  Orders  ").value()).isEqualTo("Orders");
        assertThat(new TemplateName("  Order created ").value()).isEqualTo("Order created");
        assertThat(new EnvironmentName(" staging ").value()).isEqualTo("staging");
    }

    @Test
    void rejectsBlankNames() {
        assertThatThrownBy(() -> new CollectionName(" ")).isInstanceOf(InvalidCollectionException.class);
        assertThatThrownBy(() -> new TemplateName("")).isInstanceOf(InvalidTemplateException.class);
        assertThatThrownBy(() -> new EnvironmentName("\t")).isInstanceOf(InvalidEnvironmentException.class);
    }

    @Test
    void rejectsNamesLongerThanOneHundredCharacters() {
        var tooLong = "x".repeat(101);

        assertThat(new CollectionName("x".repeat(100)).value()).hasSize(100);
        assertThatThrownBy(() -> new CollectionName(tooLong)).isInstanceOf(InvalidCollectionException.class);
        assertThatThrownBy(() -> new TemplateName(tooLong)).isInstanceOf(InvalidTemplateException.class);
        assertThatThrownBy(() -> new EnvironmentName(tooLong)).isInstanceOf(InvalidEnvironmentException.class);
    }
}
