package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Records what a stream delivers; the client leaves after a number of messages or heartbeats. */
class RecordingSink implements ConsumeSessionSink {

    final List<ReceivedMessage> messages = new ArrayList<>();
    int heartbeats;
    boolean closed;

    @Nullable ReadFailedException failure;

    private int leaveAfterMessages = Integer.MAX_VALUE;
    private int leaveAfterHeartbeats = Integer.MAX_VALUE;

    RecordingSink leavingAfterMessages(int count) {
        leaveAfterMessages = count;
        return this;
    }

    RecordingSink leavingAfterHeartbeats(int count) {
        leaveAfterHeartbeats = count;
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
}
