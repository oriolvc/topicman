package com.oriolvc.topicman.collection.application.model;

import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import com.oriolvc.topicman.collection.domain.model.Variable;
import java.util.List;

/** @param variables in order; a name may appear only once */
public record CreateEnvironmentCommand(EnvironmentName name, List<Variable> variables) {}
