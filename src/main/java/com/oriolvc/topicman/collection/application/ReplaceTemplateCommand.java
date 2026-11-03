package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.TemplateId;

public record ReplaceTemplateCommand(CollectionId collectionId, TemplateId templateId, TemplateContent content) {}
