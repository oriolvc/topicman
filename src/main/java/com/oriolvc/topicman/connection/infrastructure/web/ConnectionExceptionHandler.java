package com.oriolvc.topicman.connection.infrastructure.web;

import com.oriolvc.topicman.connection.domain.ConnectionNameTakenException;
import com.oriolvc.topicman.connection.domain.InvalidConnectionException;
import com.oriolvc.topicman.shared.domain.ConnectionNotFoundException;
import com.oriolvc.topicman.shared.infrastructure.web.ContextExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ConnectionsController.class)
class ConnectionExceptionHandler extends ContextExceptionHandler {

    static final String INVALID_REQUEST = "connection.invalid-request";

    ConnectionExceptionHandler() {
        super(INVALID_REQUEST);
    }

    @ExceptionHandler
    ProblemDetail handle(ConnectionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Connection not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(ConnectionNameTakenException exception) {
        return problem(HttpStatus.CONFLICT, "Connection name taken", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidConnectionException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid connection", exception);
    }
}
