package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.CollectionName;
import java.util.List;

/** @param templates in the file's order */
public record ImportCollectionCommand(CollectionName name, List<TemplateContent> templates) {}
