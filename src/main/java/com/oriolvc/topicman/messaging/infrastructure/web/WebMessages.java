package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.domain.Destination;
import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.PayloadFormat;
import com.oriolvc.topicman.messaging.infrastructure.web.model.ExchangeDestination;
import com.oriolvc.topicman.messaging.infrastructure.web.model.TopicDestination;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Maps between the generated messaging DTOs and the domain. */
final class WebMessages {

    private WebMessages() {}

    static Destination toDestination(com.oriolvc.topicman.messaging.infrastructure.web.model.Destination destination) {
        return switch (destination) {
            case TopicDestination topic -> Destination.topic(topic.getName());
            case ExchangeDestination exchange -> Destination.exchange(exchange.getName(), exchange.getRoutingKey());
            default ->
                throw new IllegalStateException(
                        "Unmapped destination type " + destination.getClass().getSimpleName());
        };
    }

    static com.oriolvc.topicman.messaging.infrastructure.web.model.Destination toDestinationDto(
            Destination destination) {
        return switch (destination) {
            case Destination.Topic(var name) -> new TopicDestination(TopicDestination.TypeEnum.TOPIC, name);
            case Destination.Exchange(var name, var routingKey) ->
                new ExchangeDestination(ExchangeDestination.TypeEnum.EXCHANGE, name, routingKey);
        };
    }

    static com.oriolvc.topicman.messaging.infrastructure.web.model.Payload toPayload(Payload payload) {
        return new com.oriolvc.topicman.messaging.infrastructure.web.model.Payload(
                com.oriolvc.topicman.messaging.infrastructure.web.model.Payload.FormatEnum.fromValue(
                        payload.format().name()),
                payload.content());
    }

    static Message toMessage(
            @Nullable String key,
            @Nullable Map<String, String> headers,
            com.oriolvc.topicman.messaging.infrastructure.web.model.Payload payload) {
        return new Message(
                key,
                headers == null ? Headers.none() : Headers.of(headers),
                new Payload(PayloadFormat.valueOf(payload.getFormat().getValue()), payload.getContent()));
    }
}
