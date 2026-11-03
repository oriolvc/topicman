package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.TemplateId;

public record DeleteTemplateCommand(CollectionId collectionId, TemplateId templateId) {}
