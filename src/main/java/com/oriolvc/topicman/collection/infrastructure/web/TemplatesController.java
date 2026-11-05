package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.model.AddTemplateCommand;
import com.oriolvc.topicman.collection.application.model.DeleteTemplateCommand;
import com.oriolvc.topicman.collection.application.model.ReplaceTemplateCommand;
import com.oriolvc.topicman.collection.application.model.ResolveTemplateCommand;
import com.oriolvc.topicman.collection.application.usecase.AddTemplateUseCase;
import com.oriolvc.topicman.collection.application.usecase.DeleteTemplateUseCase;
import com.oriolvc.topicman.collection.application.usecase.ReplaceTemplateUseCase;
import com.oriolvc.topicman.collection.application.usecase.ResolveTemplateUseCase;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.collection.domain.model.TemplateId;
import com.oriolvc.topicman.collection.infrastructure.web.api.TemplatesApi;
import com.oriolvc.topicman.collection.infrastructure.web.model.ResolveTemplateRequest;
import com.oriolvc.topicman.collection.infrastructure.web.model.ResolvedMessageResponse;
import com.oriolvc.topicman.collection.infrastructure.web.model.TemplateRequest;
import com.oriolvc.topicman.collection.infrastructure.web.model.TemplateResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class TemplatesController implements TemplatesApi {

    private final AddTemplateUseCase addTemplate;
    private final ReplaceTemplateUseCase replaceTemplate;
    private final DeleteTemplateUseCase deleteTemplate;
    private final ResolveTemplateUseCase resolveTemplate;

    TemplatesController(
            AddTemplateUseCase addTemplate,
            ReplaceTemplateUseCase replaceTemplate,
            DeleteTemplateUseCase deleteTemplate,
            ResolveTemplateUseCase resolveTemplate) {
        this.addTemplate = addTemplate;
        this.replaceTemplate = replaceTemplate;
        this.deleteTemplate = deleteTemplate;
        this.resolveTemplate = resolveTemplate;
    }

    @Override
    public ResponseEntity<TemplateResponse> addTemplate(Long collectionId, TemplateRequest request) {
        var result = addTemplate.execute(
                new AddTemplateCommand(new CollectionId(collectionId), WebCollections.toContent(request)));
        var location = UriComponentsBuilder.fromPath(TemplatesApi.PATH_REPLACE_TEMPLATE)
                .buildAndExpand(collectionId, result.id())
                .toUri();
        return ResponseEntity.created(location).body(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<TemplateResponse> replaceTemplate(
            Long collectionId, Long templateId, TemplateRequest request) {
        var result = replaceTemplate.execute(new ReplaceTemplateCommand(
                new CollectionId(collectionId), new TemplateId(templateId), WebCollections.toContent(request)));
        return ResponseEntity.ok(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<Void> deleteTemplate(Long collectionId, Long templateId) {
        deleteTemplate.execute(new DeleteTemplateCommand(new CollectionId(collectionId), new TemplateId(templateId)));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ResolvedMessageResponse> resolveTemplate(
            Long collectionId, Long templateId, ResolveTemplateRequest request) {
        var environmentId = request.getEnvironmentId() == null ? null : new EnvironmentId(request.getEnvironmentId());
        var resolved = resolveTemplate.execute(
                new ResolveTemplateCommand(new CollectionId(collectionId), new TemplateId(templateId), environmentId));
        return ResponseEntity.ok(WebCollections.toResponse(resolved));
    }
}
