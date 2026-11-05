package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.model.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.application.model.ImportCollectionCommand;
import com.oriolvc.topicman.collection.application.model.ListCollectionsCommand;
import com.oriolvc.topicman.collection.application.model.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.application.usecase.CreateEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.usecase.ImportCollectionUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListCollectionsUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListEnvironmentsUseCase;
import com.oriolvc.topicman.collection.domain.model.CollectionName;
import com.oriolvc.topicman.collection.domain.model.EnvironmentName;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionFile;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentFile;
import com.oriolvc.topicman.shared.infrastructure.examples.ExampleData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Comparator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Example collections and environments, stored in the same portable files users export and import
 * (ADR 0006), which makes them examples of the format too. Here because that format is a web model.
 */
@Component
class ExampleCollectionFiles implements ExampleData {

    private static final String COLLECTIONS = "classpath:examples/collections/*.topicman.yaml";
    private static final String ENVIRONMENTS = "classpath:examples/environments/*.topicman.yaml";

    private final YAMLMapper yaml = YAMLMapper.builder().build();
    private final ListCollectionsUseCase listCollections;
    private final ListEnvironmentsUseCase listEnvironments;
    private final ImportCollectionUseCase importCollection;
    private final CreateEnvironmentUseCase createEnvironment;

    ExampleCollectionFiles(
            ListCollectionsUseCase listCollections,
            ListEnvironmentsUseCase listEnvironments,
            ImportCollectionUseCase importCollection,
            CreateEnvironmentUseCase createEnvironment) {
        this.listCollections = listCollections;
        this.listEnvironments = listEnvironments;
        this.importCollection = importCollection;
        this.createEnvironment = createEnvironment;
    }

    @Override
    public boolean isEmpty() {
        return listCollections.execute(new ListCollectionsCommand()).isEmpty()
                && listEnvironments.execute(new ListEnvironmentsCommand()).isEmpty();
    }

    @Override
    public void load() {
        for (var resource : filesIn(COLLECTIONS)) {
            var file = read(resource, CollectionFile.class);
            importCollection.execute(new ImportCollectionCommand(
                    new CollectionName(file.getName()),
                    file.getTemplates().stream().map(WebCollections::toContent).toList()));
        }
        for (var resource : filesIn(ENVIRONMENTS)) {
            var file = read(resource, EnvironmentFile.class);
            createEnvironment.execute(new CreateEnvironmentCommand(
                    new EnvironmentName(file.getName()), WebCollections.toVariables(file.getVariables())));
        }
    }

    private static Resource[] filesIn(String pattern) {
        try {
            var files = new PathMatchingResourcePatternResolver().getResources(pattern);
            // The order resources are found in depends on the file system.
            Arrays.sort(files, Comparator.comparing(Resource::getFilename));
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T read(Resource resource, Class<T> type) {
        try (var content = resource.getInputStream()) {
            return yaml.readValue(content, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
