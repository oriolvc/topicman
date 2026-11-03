package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import com.oriolvc.topicman.collection.domain.Variable;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Replaces an environment's name and all its variables. */
@Service
public class ReplaceEnvironmentUseCase {

    private final EnvironmentRepository repository;
    private final Clock clock;

    public ReplaceEnvironmentUseCase(EnvironmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public EnvironmentResult execute(ReplaceEnvironmentCommand command) {
        var environment =
                repository.findById(command.id()).orElseThrow(() -> new EnvironmentNotFoundException(command.id()));
        var variables = Variable.toMap(command.variables());
        if (!command.name().equals(environment.name()) && repository.existsByName(command.name())) {
            throw new EnvironmentNameTakenException(command.name());
        }
        var now = clock.instant();
        environment.rename(command.name(), now);
        environment.replaceVariables(variables, now);
        repository.save(environment);
        return EnvironmentResult.of(environment);
    }
}
