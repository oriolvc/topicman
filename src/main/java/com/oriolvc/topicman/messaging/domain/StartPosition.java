package com.oriolvc.topicman.messaging.domain;

/** Where a consume session starts reading. */
public enum StartPosition {
    /** Only messages published after the session opens. */
    LATEST,
    /** All messages the broker still retains. */
    EARLIEST
}
