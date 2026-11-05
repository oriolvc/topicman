package com.oriolvc.topicman.shared.infrastructure.examples;

/** What a bounded context contributes to the example data, through its own use cases. */
public interface ExampleData {

    /** Whether this context has no data of its own, so loading cannot clash with a user's data. */
    boolean isEmpty();

    void load();
}
