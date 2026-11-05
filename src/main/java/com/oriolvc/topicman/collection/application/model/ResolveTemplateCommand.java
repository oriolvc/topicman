package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.collection.domain.model.TemplateId;
import org.jspecify.annotations.Nullable;

/** @param environmentId the environment to resolve against, or {@code null} for built-in variables only */
public record ResolveTemplateCommand(
        CollectionId collectionId,
        TemplateId templateId,
        @Nullable EnvironmentId environmentId) {}
