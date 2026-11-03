package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.application.CreateEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.GetCollectionCommand;
import com.oriolvc.topicman.collection.application.GetCollectionUseCase;
import com.oriolvc.topicman.collection.application.GetEnvironmentCommand;
import com.oriolvc.topicman.collection.application.GetEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.ImportCollectionCommand;
import com.oriolvc.topicman.collection.application.ImportCollectionUseCase;
import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.infrastructure.web.api.CollectionsApi;
import com.oriolvc.topicman.collection.infrastructure.web.api.EnvironmentsApi;
import com.oriolvc.topicman.collection.infrastructure.web.api.FilesApi;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionFile;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentFile;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentResponse;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/** Exports collections and environments to portable YAML files and imports them (ADR 0006). */
@RestController
class FilesController implements FilesApi {

    private static final String FILE_SUFFIX = ".topicman.yaml";

    private final GetCollectionUseCase getCollection;
    private final ImportCollectionUseCase importCollection;
    private final GetEnvironmentUseCase getEnvironment;
    private final CreateEnvironmentUseCase createEnvironment;

    FilesController(
            GetCollectionUseCase getCollection,
            ImportCollectionUseCase importCollection,
            GetEnvironmentUseCase getEnvironment,
            CreateEnvironmentUseCase createEnvironment) {
        this.getCollection = getCollection;
        this.importCollection = importCollection;
        this.getEnvironment = getEnvironment;
        this.createEnvironment = createEnvironment;
    }

    @Override
    public ResponseEntity<CollectionFile> exportCollection(Long collectionId) {
        var result = getCollection.execute(new GetCollectionCommand(new CollectionId(collectionId)));
        var file = new CollectionFile(
                CollectionFile.TopicmanEnum.NUMBER_1,
                CollectionFile.KindEnum.COLLECTION,
                result.name(),
                result.templates().stream().map(WebCollections::toFile).toList());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(result.name(), "collection"))
                .body(file);
    }

    @Override
    public ResponseEntity<CollectionResponse> importCollection(CollectionFile file, @Nullable String name) {
        var result = importCollection.execute(new ImportCollectionCommand(
                new CollectionName(name == null ? file.getName() : name),
                file.getTemplates().stream().map(WebCollections::toContent).toList()));
        var location = UriComponentsBuilder.fromPath(CollectionsApi.PATH_GET_COLLECTION)
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<EnvironmentFile> exportEnvironment(Long environmentId) {
        var result = getEnvironment.execute(new GetEnvironmentCommand(new EnvironmentId(environmentId)));
        var response = WebCollections.toResponse(result);
        var file = new EnvironmentFile(
                EnvironmentFile.TopicmanEnum.NUMBER_1,
                EnvironmentFile.KindEnum.ENVIRONMENT,
                response.getName(),
                response.getVariables());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, attachment(result.name(), "environment"))
                .body(file);
    }

    @Override
    public ResponseEntity<EnvironmentResponse> importEnvironment(EnvironmentFile file, @Nullable String name) {
        var result = createEnvironment.execute(new CreateEnvironmentCommand(
                new EnvironmentName(name == null ? file.getName() : name),
                WebCollections.toVariables(file.getVariables())));
        var location = UriComponentsBuilder.fromPath(EnvironmentsApi.PATH_GET_ENVIRONMENT)
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(WebCollections.toResponse(result));
    }

    /** A download named after what it holds, such as {@code orders-and-shipping.topicman.yaml}. */
    private static String attachment(String name, String fallback) {
        var slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return ContentDisposition.attachment()
                .filename((slug.isEmpty() ? fallback : slug) + FILE_SUFFIX)
                .build()
                .toString();
    }
}
