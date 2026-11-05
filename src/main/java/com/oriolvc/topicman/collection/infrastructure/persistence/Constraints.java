package com.oriolvc.topicman.collection.infrastructure.persistence;

import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;

final class Constraints {

    private Constraints() {}

    /** Ignores case, as H2 reports constraint names in upper case and PostgreSQL in lower case. */
    static boolean violates(DataIntegrityViolationException e, String constraint) {
        var message = e.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(constraint);
    }
}
