package com.oriolvc.topicman.collection.domain;

import com.oriolvc.topicman.shared.domain.DomainException;

/** A message template breaks a rule, such as a blank destination or an empty key. */
public final class InvalidTemplateException extends DomainException {

    public static final String CODE = "collection.invalid-template";

    InvalidTemplateException(String message) {
        super(CODE, message);
    }
}
