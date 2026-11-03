package com.oriolvc.topicman.collection.domain;

/** The name of an environment, unique among environments. Surrounding whitespace is stripped. */
public record EnvironmentName(String value) {

    public EnvironmentName {
        value = Names.require(value, "Environment", InvalidEnvironmentException::new);
    }
}
