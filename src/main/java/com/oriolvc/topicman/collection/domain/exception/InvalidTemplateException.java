package com.oriolvc.topicman.collection.domain.exception;

import com.oriolvc.topicman.shared.domain.DomainException;

/** A message template breaks a rule, such as a blank destination or an empty key. */
public final class InvalidTemplateException extends DomainException {

    public static final String CODE = "collection.invalid-template";

    public InvalidTemplateException(String message) {
        super(CODE, message);
    }
}
