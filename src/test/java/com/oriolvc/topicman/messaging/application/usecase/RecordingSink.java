package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.application.model.ConsumeSessionSink;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.model.ReceivedMessage;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Records what a stream delivers; the client leaves after a number of messages or heartbeats. Read
 * it only once its stream has stopped.
 */
class RecordingSink implements ConsumeSessionSink {

    final List<ReceivedMessage> messages = new ArrayList<>();
    int heartbeats;
    boolean closed;
    boolean replaced;

    @Nullable ReadFailedException failure;

    private int leaveAfterMessages = Integer.MAX_VALUE;
    private int leaveAfterHeartbeats = Integer.MAX_VALUE;
    private Runnable afterFirstMessage = () -> {};

    RecordingSink leavingAfterMessages(int count) {
        leaveAfterMessages = count;
        return this;
    }

    RecordingSink leavingAfterHeartbeats(int count) {
        leaveAfterHeartbeats = count;
        return this;
    }

    /** Runs {@code action} on the streaming thread once the first message is delivered. */
    RecordingSink afterFirstMessage(Runnable action) {
        afterFirstMessage = action;
        return this;
    }

    @Override
    public boolean isConnected() {
        return messages.size() < leaveAfterMessages && heartbeats < leaveAfterHeartbeats;
    }

    @Override
    public boolean message(ReceivedMessage message) {
        if (!isConnected()) {
            return false;
        }
        messages.add(message);
        if (messages.size() == 1) {
            afterFirstMessage.run();
        }
        return true;
    }

    @Override
    public boolean heartbeat() {
        if (!isConnected()) {
            return false;
        }
        heartbeats++;
        return true;
    }

    @Override
    public void failed(ReadFailedException failure) {
        this.failure = failure;
    }

    @Override
    public void closed() {
        closed = true;
    }

    @Override
    public void replaced() {
        replaced = true;
    }
}
