package com.oriolvc.topicman.messaging.application.usecase;

import com.oriolvc.topicman.messaging.application.helper.ConsumeSessionRegistry;
import com.oriolvc.topicman.messaging.application.model.CloseConsumeSessionCommand;
import com.oriolvc.topicman.messaging.domain.exception.ConsumeSessionNotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class CloseConsumeSessionUseCase {

    private final ConsumeSessionRegistry registry;
    private final Clock clock;

    public CloseConsumeSessionUseCase(ConsumeSessionRegistry registry, Clock clock) {
        this.registry = registry;
        this.clock = clock;
    }

    /** Closes the session. A client streaming it gets a {@code closed} event and its stream ends. */
    public void execute(CloseConsumeSessionCommand command) {
        var active = registry.remove(command.id()).orElseThrow(() -> new ConsumeSessionNotFoundException(command.id()));
        active.session().close(clock.instant());
        if (active.requestClose()) {
            active.stream().close();
        }
    }
}
