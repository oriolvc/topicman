package com.oriolvc.topicman.collection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariableTest {

    @Test
    void collectsVariablesInOrder() {
        var variables = Variable.toMap(
                List.of(new Variable(new VariableName("env"), "stg"), new Variable(new VariableName("orderId"), "42")));

        assertThat(variables)
                .containsExactly(
                        Map.entry(new VariableName("env"), "stg"), Map.entry(new VariableName("orderId"), "42"));
    }

    @Test
    void rejectsTheSameVariableTwice() {
        var twice = List.of(new Variable(new VariableName("env"), "stg"), new Variable(new VariableName("env"), "prd"));

        assertThatThrownBy(() -> Variable.toMap(twice))
                .isInstanceOf(InvalidEnvironmentException.class)
                .hasMessageContaining("env");
    }
}
