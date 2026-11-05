package com.oriolvc.topicman.collection.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One ordered name and value pair: a template header or an environment variable. */
@Embeddable
class NameValueEmbeddable {

    @Column(nullable = false)
    private String name;

    @Column(name = "entry_value", nullable = false)
    private String value;

    protected NameValueEmbeddable() {}

    NameValueEmbeddable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    String name() {
        return name;
    }

    String value() {
        return value;
    }
}
