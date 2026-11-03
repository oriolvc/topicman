package com.oriolvc.topicman.collection.infrastructure.web;

import com.oriolvc.topicman.collection.application.CreateEnvironmentCommand;
import com.oriolvc.topicman.collection.application.CreateEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.DeleteEnvironmentCommand;
import com.oriolvc.topicman.collection.application.DeleteEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.GetEnvironmentCommand;
import com.oriolvc.topicman.collection.application.GetEnvironmentUseCase;
import com.oriolvc.topicman.collection.application.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.application.ListEnvironmentsUseCase;
import com.oriolvc.topicman.collection.application.ReplaceEnvironmentCommand;
import com.oriolvc.topicman.collection.application.ReplaceEnvironmentUseCase;
import com.oriolvc.topicman.collection.domain.EnvironmentId;
import com.oriolvc.topicman.collection.domain.EnvironmentName;
import com.oriolvc.topicman.collection.infrastructure.web.api.EnvironmentsApi;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentRequest;
import com.oriolvc.topicman.collection.infrastructure.web.model.EnvironmentResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class EnvironmentsController implements EnvironmentsApi {

    private final ListEnvironmentsUseCase listEnvironments;
    private final CreateEnvironmentUseCase createEnvironment;
    private final GetEnvironmentUseCase getEnvironment;
    private final ReplaceEnvironmentUseCase replaceEnvironment;
    private final DeleteEnvironmentUseCase deleteEnvironment;

    EnvironmentsController(
            ListEnvironmentsUseCase listEnvironments,
            CreateEnvironmentUseCase createEnvironment,
            GetEnvironmentUseCase getEnvironment,
            ReplaceEnvironmentUseCase replaceEnvironment,
            DeleteEnvironmentUseCase deleteEnvironment) {
        this.listEnvironments = listEnvironments;
        this.createEnvironment = createEnvironment;
        this.getEnvironment = getEnvironment;
        this.replaceEnvironment = replaceEnvironment;
        this.deleteEnvironment = deleteEnvironment;
    }

    @Override
    public ResponseEntity<List<EnvironmentResponse>> listEnvironments() {
        return ResponseEntity.ok(listEnvironments.execute(new ListEnvironmentsCommand()).stream()
                .map(WebCollections::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<EnvironmentResponse> createEnvironment(EnvironmentRequest request) {
        var result = createEnvironment.execute(new CreateEnvironmentCommand(
                new EnvironmentName(request.getName()), WebCollections.toVariables(request.getVariables())));
        var location = UriComponentsBuilder.fromPath(EnvironmentsApi.PATH_GET_ENVIRONMENT)
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<EnvironmentResponse> getEnvironment(Long environmentId) {
        return ResponseEntity.ok(WebCollections.toResponse(
                getEnvironment.execute(new GetEnvironmentCommand(new EnvironmentId(environmentId)))));
    }

    @Override
    public ResponseEntity<EnvironmentResponse> replaceEnvironment(Long environmentId, EnvironmentRequest request) {
        var result = replaceEnvironment.execute(new ReplaceEnvironmentCommand(
                new EnvironmentId(environmentId),
                new EnvironmentName(request.getName()),
                WebCollections.toVariables(request.getVariables())));
        return ResponseEntity.ok(WebCollections.toResponse(result));
    }

    @Override
    public ResponseEntity<Void> deleteEnvironment(Long environmentId) {
        deleteEnvironment.execute(new DeleteEnvironmentCommand(new EnvironmentId(environmentId)));
        return ResponseEntity.noContent().build();
    }
}
