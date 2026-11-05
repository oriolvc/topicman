package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionId;

public record AddTemplateCommand(CollectionId collectionId, TemplateContent content) {}
