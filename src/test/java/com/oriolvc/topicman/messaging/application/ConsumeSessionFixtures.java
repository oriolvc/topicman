package com.oriolvc.topicman.messaging.application;

import com.oriolvc.topicman.messaging.domain.Headers;
import com.oriolvc.topicman.messaging.domain.Message;
import com.oriolvc.topicman.messaging.domain.Payload;
import com.oriolvc.topicman.messaging.domain.ReceivedMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

final class ConsumeSessionFixtures {

    static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    static final ConsumeSessionLimits LIMITS = new ConsumeSessionLimits(20, Duration.ofMinutes(5));

    private ConsumeSessionFixtures() {}

    static ReceivedMessage aReceivedText(String content, String position) {
        return new ReceivedMessage(new Message(null, Headers.none(), Payload.text(content)), NOW, position);
    }

    /** Runs nothing until told to, so a test can act while a stream is claimed but not running. */
    static final class CapturingExecutor implements Executor {

        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable task) {
            tasks.add(task);
        }

        void runAll() {
            var pending = List.copyOf(tasks);
            tasks.clear();
            pending.forEach(Runnable::run);
        }
    }
}
