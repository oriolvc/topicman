package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.domain.exception.CollectionNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.exception.InvalidCollectionException;
import com.oriolvc.topicman.collection.domain.exception.InvalidEnvironmentException;
import com.oriolvc.topicman.collection.domain.exception.InvalidTemplateException;
import com.oriolvc.topicman.collection.domain.exception.TemplateNameTakenException;
import com.oriolvc.topicman.collection.domain.exception.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.exception.UnresolvedVariablesException;
import com.oriolvc.topicman.shared.infrastructure.web.ContextExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = CollectionsController.class)
class CollectionExceptionHandler extends ContextExceptionHandler {

    static final String INVALID_REQUEST = "collection.invalid-request";

    CollectionExceptionHandler() {
        super(INVALID_REQUEST);
    }

    @ExceptionHandler
    ProblemDetail handle(CollectionNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Collection not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(TemplateNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Template not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(EnvironmentNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Environment not found", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(CollectionNameTakenException exception) {
        return problem(HttpStatus.CONFLICT, "Collection name taken", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(TemplateNameTakenException exception) {
        return problem(HttpStatus.CONFLICT, "Template name taken", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(EnvironmentNameTakenException exception) {
        return problem(HttpStatus.CONFLICT, "Environment name taken", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidCollectionException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid collection", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidTemplateException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid template", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidEnvironmentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid environment", exception);
    }

    @ExceptionHandler
    ProblemDetail handle(UnresolvedVariablesException exception) {
        var problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "Unresolved variables", exception);
        problem.setProperty("missing", exception.missing());
        problem.setProperty("unknownBuiltIns", exception.unknownBuiltIns());
        return problem;
    }
}
