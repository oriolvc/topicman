package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionNotFoundException;
import com.oriolvc.topicman.collection.domain.CollectionRepository;
import com.oriolvc.topicman.collection.domain.DynamicValues;
import com.oriolvc.topicman.collection.domain.Environment;
import com.oriolvc.topicman.collection.domain.EnvironmentNotFoundException;
import com.oriolvc.topicman.collection.domain.EnvironmentRepository;
import com.oriolvc.topicman.collection.domain.ResolvedMessage;
import com.oriolvc.topicman.collection.domain.TemplateNotFoundException;
import com.oriolvc.topicman.collection.domain.TemplateResolver;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a template's placeholders without producing anything; the client produces the result
 * through a connection of the template's broker type (ADR 0006).
 */
@Service
public class ResolveTemplateUseCase {

    private final CollectionRepository collections;
    private final EnvironmentRepository environments;
    private final TemplateResolver resolver;

    public ResolveTemplateUseCase(
            CollectionRepository collections, EnvironmentRepository environments, DynamicValues dynamicValues) {
        this.collections = collections;
        this.environments = environments;
        this.resolver = new TemplateResolver(dynamicValues);
    }

    @Transactional(readOnly = true)
    public ResolvedMessage execute(ResolveTemplateCommand command) {
        var collection = collections
                .findById(command.collectionId())
                .orElseThrow(() -> new CollectionNotFoundException(command.collectionId()));
        var template = collection
                .template(command.templateId())
                .orElseThrow(() -> new TemplateNotFoundException(command.templateId()));
        return resolver.resolve(template, environmentOf(command));
    }

    private @Nullable Environment environmentOf(ResolveTemplateCommand command) {
        var id = command.environmentId();
        if (id == null) {
            return null;
        }
        return environments.findById(id).orElseThrow(() -> new EnvironmentNotFoundException(id));
    }
}
