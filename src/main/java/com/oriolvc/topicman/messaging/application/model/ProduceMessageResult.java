package com.oriolvc.topicman.messaging.application.model;

import com.oriolvc.topicman.messaging.domain.model.SendReceipt;
import java.util.List;

/** The broker's acknowledgement of each copy, in the order they were sent. */
public record ProduceMessageResult(List<SendReceipt> receipts) {

    public ProduceMessageResult {
        receipts = List.copyOf(receipts);
    }
}
