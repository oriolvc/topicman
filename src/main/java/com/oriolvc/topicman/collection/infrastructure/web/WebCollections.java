package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.CollectionResult;
import com.oriolvc.topicman.collection.application.EnvironmentResult;
import com.oriolvc.topicman.collection.application.TemplateContent;
import com.oriolvc.topicman.collection.application.TemplateResult;
import com.oriolvc.topicman.collection.domain.PayloadFormat;
import com.oriolvc.topicman.collection.domain.ResolvedMessage;
import com.oriolvc.topicman.collection.domain.TemplateDestination;
import com.oriolvc.topicman.collection.domain.TemplateName;
import com.oriolvc.topicman.collection.domain.TemplatePayload;
import com.oriolvc.topicman.collection.domain.Variable;
import com.oriolvc.topicman.collection.domain.VariableName;
import com.oriolvc.topicman.collection.infrastructure.web.model.BrokerType;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.Destination;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.ExchangeDestination;
import com.oriolvc.topicman.collection.infrastructure.web.model.Payload;
import com.oriolvc.topicman.collection.infrastructure.web.model.ResolvedMessageResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.TemplateFile;
import com.oriolvc.topicman.collection.infrastructure.web.model.TemplateRequest;
import com.oriolvc.topicman.collection.infrastructure.web.model.TemplateResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.TopicDestination;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Maps between the generated collection DTOs and the application layer. */
final class WebCollections {

    private WebCollections() {}

    static TemplateContent toContent(TemplateRequest request) {
        return toContent(
                request.getName(),
                request.getBrokerType(),
                request.getDestination(),
                request.getKey(),
                request.getHeaders(),
                request.getPayload());
    }

    static TemplateContent toContent(TemplateFile file) {
        return toContent(
                file.getName(),
                file.getBrokerType(),
                file.getDestination(),
                file.getKey(),
                file.getHeaders(),
                file.getPayload());
    }

    private static TemplateContent toContent(
            String name,
            BrokerType brokerType,
            Destination destination,
            @Nullable String key,
            Map<String, String> headers,
            Payload payload) {
        return new TemplateContent(
                new TemplateName(name),
                com.oriolvc.topicman.shared.domain.BrokerType.valueOf(brokerType.getValue()),
                toDestination(destination),
                key,
                headers,
                new TemplatePayload(PayloadFormat.valueOf(payload.getFormat().getValue()), payload.getContent()));
    }

    /** A template as it is written to a file: everything but its id. */
    static TemplateFile toFile(TemplateResult result) {
        return new TemplateFile(
                        result.name(),
                        BrokerType.fromValue(result.brokerType().name()),
                        toDestinationDto(result.destination()),
                        toPayloadDto(result.payload()))
                .key(result.key())
                .headers(new LinkedHashMap<>(result.headers()));
    }

    static List<Variable> toVariables(
            List<com.oriolvc.topicman.collection.infrastructure.web.model.Variable> variables) {
        return variables.stream()
                .map(variable -> new Variable(new VariableName(variable.getName()), variable.getValue()))
                .toList();
    }

    static CollectionResponse toResponse(CollectionResult result) {
        return new CollectionResponse(
                result.id(),
                result.name(),
                result.templates().stream().map(WebCollections::toResponse).toList(),
                utc(result.createdAt()),
                utc(result.updatedAt()));
    }

    static TemplateResponse toResponse(TemplateResult result) {
        return new TemplateResponse(
                        result.id(),
                        result.name(),
                        BrokerType.fromValue(result.brokerType().name()),
                        toDestinationDto(result.destination()),
                        new LinkedHashMap<>(result.headers()),
                        toPayloadDto(result.payload()))
                .key(result.key());
    }

    static EnvironmentResponse toResponse(EnvironmentResult result) {
        return new EnvironmentResponse(
                result.id(),
                result.name(),
                result.variables().entrySet().stream()
                        .map(variable -> new com.oriolvc.topicman.collection.infrastructure.web.model.Variable(
                                variable.getKey(), variable.getValue()))
                        .toList(),
                utc(result.createdAt()),
                utc(result.updatedAt()));
    }

    static ResolvedMessageResponse toResponse(ResolvedMessage message) {
        return new ResolvedMessageResponse(
                        BrokerType.fromValue(message.brokerType().name()),
                        toDestinationDto(message.destination()),
                        new LinkedHashMap<>(message.headers()),
                        toPayloadDto(message.payload()))
                .key(message.key());
    }

    static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static TemplateDestination toDestination(Destination destination) {
        return switch (destination) {
            case TopicDestination topic -> TemplateDestination.topic(topic.getName());
            case ExchangeDestination exchange ->
                TemplateDestination.exchange(exchange.getName(), exchange.getRoutingKey());
            default ->
                throw new IllegalStateException(
                        "Unmapped destination type " + destination.getClass().getSimpleName());
        };
    }

    private static Destination toDestinationDto(TemplateDestination destination) {
        return switch (destination) {
            case TemplateDestination.Topic(var name) -> new TopicDestination(TopicDestination.TypeEnum.TOPIC, name);
            case TemplateDestination.Exchange(var name, var routingKey) ->
                new ExchangeDestination(ExchangeDestination.TypeEnum.EXCHANGE, name, routingKey);
        };
    }

    private static Payload toPayloadDto(TemplatePayload payload) {
        return new Payload(Payload.FormatEnum.fromValue(payload.format().name()), payload.content());
    }
}
