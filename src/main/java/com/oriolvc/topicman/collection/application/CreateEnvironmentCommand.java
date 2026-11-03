package com.oriolvc.topicman.collection.application;

import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.domain.Variable;
import java.util.List;

/** @param variables in order; a name may appear only once */
public record CreateEnvironmentCommand(EnvironmentName name, List<Variable> variables) {}
