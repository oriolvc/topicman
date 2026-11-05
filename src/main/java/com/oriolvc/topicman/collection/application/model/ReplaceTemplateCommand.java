package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.TemplateId;

public record ReplaceTemplateCommand(CollectionId collectionId, TemplateId templateId, TemplateContent content) {}
