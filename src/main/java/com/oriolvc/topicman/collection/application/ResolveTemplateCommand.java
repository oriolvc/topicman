package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.TemplateId;
import org.jspecify.annotations.Nullable;

/** @param environmentId the environment to resolve against, or {@code null} for built-in variables only */
public record ResolveTemplateCommand(
        CollectionId collectionId,
        TemplateId templateId,
        @Nullable EnvironmentId environmentId) {}
