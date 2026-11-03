package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.Environment;
import com.oriolvc.topicman.collection.domain.EnvironmentNameTakenException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import com.oriolvc.topicman.collection.domain.Variable;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateEnvironmentUseCase {

    private final EnvironmentRepository repository;
    private final Clock clock;

    public CreateEnvironmentUseCase(EnvironmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public EnvironmentResult execute(CreateEnvironmentCommand command) {
        var variables = Variable.toMap(command.variables());
        if (repository.existsByName(command.name())) {
            throw new EnvironmentNameTakenException(command.name());
        }
        var environment = Environment.create(repository.nextId(), command.name(), variables, clock.instant());
        repository.save(environment);
        return EnvironmentResult.of(environment);
    }
}
