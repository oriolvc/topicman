package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.SendReceipt;
import java.util.List;

/** The broker's acknowledgement of each copy, in the order they were sent. */
public record ProduceMessageResult(List<SendReceipt> receipts) {

    public ProduceMessageResult {
        receipts = List.copyOf(receipts);
    }
}
