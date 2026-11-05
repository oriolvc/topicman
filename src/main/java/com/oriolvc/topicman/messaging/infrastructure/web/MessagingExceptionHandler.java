package com.oriolvc.topicman.messaging.infrastructure.web;

import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionInUseException;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionLimitReachedException;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionNotFoundException;
import com.oriolvc.topicman.messaging.domain.exception.InvalidDestinationException;
import com.oriolvc.topicman.messaging.domain.exception.InvalidMessageException;
import com.oriolvc.topicman.messaging.domain.exception.ProduceFailedException;
import com.oriolvc.topicman.messaging.domain.exception.ReadFailedException;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.infrastructure.web.ContextExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = MessagesController.class)
class MessagingExceptionHandler extends ContextExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingExceptionHandler.class);

    MessagingExceptionHandler() {
        super("messaging.invalid-request");
    }

    @ExceptionHandler
    ProblemDetail handle(ConnectionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Connection not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidMessageException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid message", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidDestinationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid destination", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ConsumeSessionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Consume session not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ConsumeSessionInUseException exception) {
        return problem(HttpStatus.CONFLICT, "Consume session in use", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ConsumeSessionLimitReachedException exception) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many consume sessions", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ReadFailedException exception) {
        log.warn("Read failed: {}", exception.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Read failed", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ProduceFailedException exception) {
        log.warn("Produce failed after {} acknowledged copies: {}", exception.sent(), exception.getMessage());
        var problem = problem(HttpStatus.SERVICE_UNAVAILABLE, "Produce failed", exception);
        problem.setProperty("sent", exception.sent());
        return problem;
    }
}
