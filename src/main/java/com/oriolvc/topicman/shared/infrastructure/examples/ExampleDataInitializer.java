package com.oriolvc.topicman.shared.infrastructure.examples;

import com.oriolvc.topicman.shared.infrastructure.EmbeddedDatabase;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads the example data at startup when it is wanted, at most once per database. A database that
 * already holds data is marked without adding to it, and the mark is what keeps deleted examples
 * from coming back. All of it runs in one transaction, so a failure leaves nothing half loaded.
 */
@Component
class ExampleDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleDataInitializer.class);
    private static final int ONLY_ROW = 1;

    private final List<ExampleData> examples;
    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final boolean wanted;

    ExampleDataInitializer(
            List<ExampleData> examples,
            JdbcClient jdbc,
            TransactionTemplate transaction,
            Clock clock,
            JdbcConnectionDetails database,
            @Value("${topicman.seed-examples:}") @Nullable Boolean setting) {
        this.examples = examples;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.clock = clock;
        this.wanted = wanted(setting, database.getJdbcUrl());
    }

    /** Unset means yes on the embedded database, for trying TopicMan out, and never on PostgreSQL. */
    static boolean wanted(@Nullable Boolean setting, String jdbcUrl) {
        return setting != null ? setting : EmbeddedDatabase.isAt(jdbcUrl);
    }

    @Override
    public void run(@Nullable ApplicationArguments arguments) {
        if (wanted) {
            transaction.executeWithoutResult(status -> loadOnce());
        }
    }

    private void loadOnce() {
        if (alreadyDone()) {
            return;
        }
        if (examples.stream().allMatch(ExampleData::isEmpty)) {
            examples.forEach(ExampleData::load);
            LOGGER.info("Loaded the example data. Delete what you do not need: it does not come back.");
        }
        jdbc.sql("INSERT INTO example_data_load (id, loaded_at) VALUES (?, ?)")
                .params(ONLY_ROW, OffsetDateTime.now(clock))
                .update();
    }

    private boolean alreadyDone() {
        return jdbc.sql("SELECT COUNT(*) FROM example_data_load")
                        .query(Long.class)
                        .single()
                > 0;
    }
}
