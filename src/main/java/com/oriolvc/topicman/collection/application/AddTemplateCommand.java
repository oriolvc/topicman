package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionId;

public record AddTemplateCommand(CollectionId collectionId, TemplateContent content) {}
