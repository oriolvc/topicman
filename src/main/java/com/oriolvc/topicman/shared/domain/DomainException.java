package com.oriolvc.topicman.shared.domain;

/**
 * Base class for business rule violations. The code is stable and part of the API contract: the
 * web layer maps it to a {@code ProblemDetail}, so never change an existing code.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected DomainException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
