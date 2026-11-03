package com.oriolvc.topicman.collection.infrastructure.persistence;

import org.springframework.dao.DataIntegrityViolationException;

final class Constraints {

    private Constraints() {}

    static boolean violates(DataIntegrityViolationException e, String constraint) {
        var message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraint);
    }
}
