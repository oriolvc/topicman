package com.oriolvc.topicman.messaging.domain;

/** How the payload is meant to be read; JSON payloads are checked for well-formedness on produce. */
public enum PayloadFormat {
    JSON,
    TEXT
}
