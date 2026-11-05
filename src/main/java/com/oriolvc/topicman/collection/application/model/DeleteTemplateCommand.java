package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.TemplateId;

public record DeleteTemplateCommand(CollectionId collectionId, TemplateId templateId) {}
