package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.BrokerEndpoint;
import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.MessageReader;
import com.oriolvc.topicman.messaging.domain.MessageStream;
import com.oriolvc.topicman.messaging.domain.ReadFailedException;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import com.oriolvc.topicman.messaging.domain.StartPosition;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Opens scripted streams: each poll returns the next batch, then nothing, or fails when told to. */
class FakeMessageReader implements MessageReader {

    record Opened(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition) {}

    private final BrokerType brokerType;
    private final List<Opened> opened = new ArrayList<>();
    private final List<FakeStream> streams = new ArrayList<>();
    private @Nullable ReadFailedException openFailure;

    FakeMessageReader(BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    void failToOpen(ReadFailedException failure) {
        openFailure = failure;
    }

    List<Opened> opened() {
        return opened;
    }

    FakeStream lastStream() {
        return streams.getLast();
    }

    @Override
    public BrokerType brokerType() {
        return brokerType;
    }

    @Override
    public MessageStream open(BrokerEndpoint endpoint, Destination destination, StartPosition startPosition) {
        if (openFailure != null) {
            throw openFailure;
        }
        opened.add(new Opened(endpoint, destination, startPosition));
        var stream = new FakeStream();
        streams.add(stream);
        return stream;
    }

    static final class FakeStream implements MessageStream {

        private final Deque<List<ReceivedMessage>> batches = new ArrayDeque<>();
        private @Nullable ReadFailedException pollFailure;
        private boolean closed;
        private int polls;

        void willReturn(List<ReceivedMessage> batch) {
            batches.add(batch);
        }

        void willFail(ReadFailedException failure) {
            pollFailure = failure;
        }

        boolean isClosed() {
            return closed;
        }

        int polls() {
            return polls;
        }

        @Override
        public List<ReceivedMessage> poll(Duration timeout) {
            if (closed) {
                throw new IllegalStateException("Polled a closed stream");
            }
            polls++;
            if (batches.isEmpty() && pollFailure != null) {
                throw pollFailure;
            }
            return batches.isEmpty() ? List.of() : batches.poll();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
