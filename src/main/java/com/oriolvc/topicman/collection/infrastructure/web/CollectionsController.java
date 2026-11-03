package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.CollectionSummaryResult;
import com.oriolvc.topicman.collection.application.CreateCollectionCommand;
import com.oriolvc.topicman.collection.application.CreateCollectionUseCase;
import com.oriolvc.topicman.collection.application.DeleteCollectionCommand;
import com.oriolvc.topicman.collection.application.DeleteCollectionUseCase;
import com.oriolvc.topicman.collection.application.GetCollectionCommand;
import com.oriolvc.topicman.collection.application.GetCollectionUseCase;
import com.oriolvc.topicman.collection.application.ListCollectionsCommand;
import com.oriolvc.topicman.collection.application.ListCollectionsUseCase;
import com.oriolvc.topicman.collection.application.RenameCollectionCommand;
import com.oriolvc.topicman.collection.application.RenameCollectionUseCase;
import com.oriolvc.topicman.collection.domain.CollectionId;
import com.oriolvc.topicman.collection.domain.CollectionName;
import com.oriolvc.topicman.collection.infrastructure.web.api.CollectionsApi;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionRequest;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.CollectionSummaryResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class CollectionsController implements CollectionsApi {

    private final ListCollectionsUseCase listCollections;
    private final CreateCollectionUseCase createCollection;
    private final GetCollectionUseCase getCollection;
    private final RenameCollectionUseCase renameCollection;
    private final DeleteCollectionUseCase deleteCollection;

    CollectionsController(
            ListCollectionsUseCase listCollections,
            CreateCollectionUseCase createCollection,
            GetCollectionUseCase getCollection,
            RenameCollectionUseCase renameCollection,
            DeleteCollectionUseCase deleteCollection) {
        this.listCollections = listCollections;
        this.createCollection = createCollection;
        this.getCollection = getCollection;
        this.renameCollection = renameCollection;
        this.deleteCollection = deleteCollection;
    }

    @Override
    public ResponseEntity<List<CollectionSummaryResponse>> listCollections() {
        return ResponseEntity.ok(listCollections.execute(new ListCollectionsCommand()).stream()
                .map(CollectionsController::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<CollectionResponse> createCollection(CollectionRequest request) {
        var result = createCollection.execute(new CreateCollectionCommand(new CollectionName(request.getName())));
        var location = UriComponentsBuilder.fromPath(CollectionsApi.PATH_GET_COLLECTION)
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<CollectionResponse> getCollection(Long collectionId) {
        return ResponseEntity.ok(WebCollections.toResponse(
                getCollection.execute(new GetCollectionCommand(new CollectionId(collectionId)))));
    }

    @Override
    public ResponseEntity<CollectionResponse> renameCollection(Long collectionId, CollectionRequest request) {
        return ResponseEntity.ok(WebCollections.toResponse(renameCollection.execute(
                new RenameCollectionCommand(new CollectionId(collectionId), new CollectionName(request.getName())))));
    }

    @Override
    public ResponseEntity<Void> deleteCollection(Long collectionId) {
        deleteCollection.execute(new DeleteCollectionCommand(new CollectionId(collectionId)));
        return ResponseEntity.noContent().build();
    }

    private static CollectionSummaryResponse toResponse(CollectionSummaryResult result) {
        return new CollectionSummaryResponse(
                result.id(),
                result.name(),
                result.templateCount(),
                WebCollections.utc(result.createdAt()),
                WebCollections.utc(result.updatedAt()));
    }
}
