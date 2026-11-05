package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.CollectionName;
import java.util.List;

/** @param templates in the file's order */
public record ImportCollectionCommand(CollectionName name, List<TemplateContent> templates) {}
