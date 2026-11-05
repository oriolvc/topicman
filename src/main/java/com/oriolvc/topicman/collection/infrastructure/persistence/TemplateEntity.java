package com.oriolvc.topicman.collection.infrastructure.persistence;

import com.oriolvc.topicman.collection.domain.model.DestinationKind;
import com.oriolvc.topicman.collection.domain.model.PayloadFormat;
import com.oriolvc.topicman.shared.domain.BrokerType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "message_template")
class TemplateEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false)
    private BrokerType brokerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_kind", nullable = false)
    private DestinationKind destinationKind;

    @Column(name = "destination_name", nullable = false)
    private String destinationName;

    @Column(name = "routing_key")
    private @Nullable String routingKey;

    @Column(name = "message_key")
    private @Nullable String messageKey;

    @ElementCollection
    @CollectionTable(name = "message_template_header", joinColumns = @JoinColumn(name = "template_id"))
    @OrderColumn(name = "sort_order")
    private List<NameValueEmbeddable> headers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "payload_format", nullable = false)
    private PayloadFormat payloadFormat;

    @Column(name = "payload_content", nullable = false)
    private String payloadContent;

    protected TemplateEntity() {}

    TemplateEntity(
            long id,
            String name,
            BrokerType brokerType,
            DestinationKind destinationKind,
            String destinationName,
            @Nullable String routingKey,
            @Nullable String messageKey,
            List<NameValueEmbeddable> headers,
            PayloadFormat payloadFormat,
            String payloadContent) {
        this.id = id;
        this.name = name;
        this.brokerType = brokerType;
        this.destinationKind = destinationKind;
        this.destinationName = destinationName;
        this.routingKey = routingKey;
        this.messageKey = messageKey;
        this.headers = new ArrayList<>(headers);
        this.payloadFormat = payloadFormat;
        this.payloadContent = payloadContent;
    }

    /** Takes every column of another entity with the same id, so this managed row is updated. */
    void copyFrom(TemplateEntity other) {
        name = other.name;
        brokerType = other.brokerType;
        destinationKind = other.destinationKind;
        destinationName = other.destinationName;
        routingKey = other.routingKey;
        messageKey = other.messageKey;
        headers.clear();
        headers.addAll(other.headers);
        payloadFormat = other.payloadFormat;
        payloadContent = other.payloadContent;
    }

    long id() {
        return id;
    }

    String name() {
        return name;
    }

    BrokerType brokerType() {
        return brokerType;
    }

    DestinationKind destinationKind() {
        return destinationKind;
    }

    String destinationName() {
        return destinationName;
    }

    @Nullable String routingKey() {
        return routingKey;
    }

    @Nullable String messageKey() {
        return messageKey;
    }

    List<NameValueEmbeddable> headers() {
        return headers;
    }

    PayloadFormat payloadFormat() {
        return payloadFormat;
    }

    String payloadContent() {
        return payloadContent;
    }
}
