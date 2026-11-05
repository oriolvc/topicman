package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.CollectionName;

public record RenameCollectionCommand(CollectionId id, CollectionName name) {}
