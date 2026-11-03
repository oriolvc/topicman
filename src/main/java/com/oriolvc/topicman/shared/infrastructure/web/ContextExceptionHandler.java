package com.oriolvc.topicman.shared.infrastructure.web;

import com.oriolvc.topicman.shared.domain.DomainException;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Base for each context's {@code @RestControllerAdvice}: every error is an RFC 9457 problem with a
 * stable {@code code}. Requests Spring cannot bind (malformed JSON, schema violations) get the
 * context's invalid-request code. Problems never echo request content, which may hold credentials:
 * validation errors list field names and messages, never rejected values.
 */
public abstract class ContextExceptionHandler extends ResponseEntityExceptionHandler {

    private final String invalidRequestCode;

    protected ContextExceptionHandler(String invalidRequestCode) {
        this.invalidRequestCode = invalidRequestCode;
    }

    /** A problem whose detail is the domain exception's message, which never holds credentials. */
    protected static ProblemDetail problem(HttpStatus status, String title, DomainException exception) {
        var problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setProperty("code", exception.code());
        return problem;
    }

    /**
     * Path and query parameters that break the spec's constraints, such as an id below 1, which
     * Spring's method validation reports as a constraint violation.
     */
    @ExceptionHandler
    ProblemDetail handle(ConstraintViolationException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
        problem.setProperty("code", invalidRequestCode);
        problem.setProperty(
                "errors",
                exception.getConstraintViolations().stream()
                        .map(violation -> Map.of(
                                "field", lastNodeOf(violation.getPropertyPath().toString()),
                                "message", violation.getMessage()))
                        .toList());
        return problem;
    }

    // "getConnection.connectionId" names the parameter after the method; clients only need the parameter.
    private static String lastNodeOf(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = exception.getBody();
        problem.setProperty(
                "errors",
                exception.getFieldErrors().stream()
                        .map(error ->
                                Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var response = super.handleExceptionInternal(exception, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem && status.is4xxClientError()) {
            problem.setProperty("code", invalidRequestCode);
        }
        return response;
    }
}
