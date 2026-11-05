package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.application.model.ConsumeSessionLimits;
import com.oriolvc.topicman.messaging.application.model.ExpireIdleConsumeSessionsCommand;
import com.oriolvc.topicman.messaging.application.model.ExpireIdleConsumeSessionsResult;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** Closes the sessions no client streamed for the idle timeout, releasing their broker clients. */
@Service
public class ExpireIdleConsumeSessionsUseCase {

    private final ConsumeSessionRegistry registry;
    private final ConsumeSessionLimits limits;
    private final Clock clock;

    public ExpireIdleConsumeSessionsUseCase(ConsumeSessionRegistry registry, ConsumeSessionLimits limits, Clock clock) {
        this.registry = registry;
        this.limits = limits;
        this.clock = clock;
    }

    public ExpireIdleConsumeSessionsResult execute(ExpireIdleConsumeSessionsCommand command) {
        var now = clock.instant();
        var expired = 0;
        for (var active : registry.all()) {
            if (active.expireIfIdle(limits.idleTimeout(), now)) {
                registry.remove(active.session().id());
                active.session().close(now);
                active.stream().close();
                expired++;
            }
        }
        return new ExpireIdleConsumeSessionsResult(expired);
    }
}
