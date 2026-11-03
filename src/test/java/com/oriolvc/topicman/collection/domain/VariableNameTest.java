package com.oriolvc.topicman.collection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class VariableNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"orderId", "_private", "kafka.topic", "env-name_2", "A"})
    void acceptsLettersDigitsUnderscoresDotsAndDashes(String name) {
        assertThat(new VariableName(name).value()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "1st", "-x", ".x", "order id", "{{x}}", "a/b"})
    void rejectsNamesThatCannotAppearInAPlaceholder(String name) {
        assertThatThrownBy(() -> new VariableName(name)).isInstanceOf(InvalidEnvironmentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"$uuid", "$custom"})
    void reservesTheDollarPrefixForBuiltInVariables(String name) {
        assertThatThrownBy(() -> new VariableName(name))
                .isInstanceOf(InvalidEnvironmentException.class)
                .hasMessageContaining("built-in");
    }
}
