package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;

public record RenameCollectionCommand(CollectionId id, CollectionName name) {}
