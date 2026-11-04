package com.oriolvc.topicman.connection.infrastructure.web;

import com.oriolvc.topicman.connection.application.ConnectionResult;
import com.oriolvc.topicman.connection.application.CreateConnectionCommand;
import com.oriolvc.topicman.connection.application.CreateConnectionUseCase;
import com.oriolvc.topicman.connection.application.DeleteConnectionCommand;
import com.oriolvc.topicman.connection.application.DeleteConnectionUseCase;
import com.oriolvc.topicman.connection.application.GetConnectionCommand;
import com.oriolvc.topicman.connection.application.GetConnectionUseCase;
import com.oriolvc.topicman.connection.application.ListConnectionsCommand;
import com.oriolvc.topicman.connection.application.ListConnectionsUseCase;
import com.oriolvc.topicman.connection.application.UpdateConnectionCommand;
import com.oriolvc.topicman.connection.application.UpdateConnectionUseCase;
import com.oriolvc.topicman.connection.domain.BrokerAddress;
import com.oriolvc.topicman.connection.domain.ConnectionName;
import com.oriolvc.topicman.connection.domain.Credentials;
import com.oriolvc.topicman.connection.domain.TlsSettings;
import com.oriolvc.topicman.connection.domain.TrustedCertificates;
import com.oriolvc.topicman.connection.infrastructure.web.api.ConnectionsApi;
import com.oriolvc.topicman.connection.infrastructure.web.model.BrokerType;
import com.oriolvc.topicman.connection.infrastructure.web.model.ConnectionResponse;
import com.oriolvc.topicman.connection.infrastructure.web.model.CreateConnectionRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.CredentialsRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.CredentialsType;
import com.oriolvc.topicman.connection.infrastructure.web.model.NoCredentialsRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.SaslMechanism;
import com.oriolvc.topicman.connection.infrastructure.web.model.TokenCredentialsRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.UpdateConnectionRequest;
import com.oriolvc.topicman.connection.infrastructure.web.model.UsernamePasswordCredentialsRequest;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.Secret;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class ConnectionsController implements ConnectionsApi {

    private final ListConnectionsUseCase listConnections;
    private final CreateConnectionUseCase createConnection;
    private final GetConnectionUseCase getConnection;
    private final UpdateConnectionUseCase updateConnection;
    private final DeleteConnectionUseCase deleteConnection;

    ConnectionsController(
            ListConnectionsUseCase listConnections,
            CreateConnectionUseCase createConnection,
            GetConnectionUseCase getConnection,
            UpdateConnectionUseCase updateConnection,
            DeleteConnectionUseCase deleteConnection) {
        this.listConnections = listConnections;
        this.createConnection = createConnection;
        this.getConnection = getConnection;
        this.updateConnection = updateConnection;
        this.deleteConnection = deleteConnection;
    }

    @Override
    public ResponseEntity<List<ConnectionResponse>> listConnections() {
        return ResponseEntity.ok(listConnections.execute(new ListConnectionsCommand()).stream()
                .map(ConnectionsController::toResponse)
                .toList());
    }

    @Override
    public ResponseEntity<ConnectionResponse> createConnection(CreateConnectionRequest request) {
        var result = createConnection.execute(new CreateConnectionCommand(
                new ConnectionName(request.getName()),
                toBrokerType(request.getBrokerType()),
                new BrokerAddress(request.getAddress()),
                toCredentials(request.getCredentials()),
                request.getTls() == null ? TlsSettings.disabled() : toTls(request.getTls())));
        var location = UriComponentsBuilder.fromPath(ConnectionsApi.PATH_GET_CONNECTION)
                .buildAndExpand(result.id())
                .toUri();
        return ResponseEntity.created(location).body(toResponse(result));
    }

    @Override
    public ResponseEntity<ConnectionResponse> getConnection(Long connectionId) {
        return ResponseEntity.ok(
                toResponse(getConnection.execute(new GetConnectionCommand(new ConnectionId(connectionId)))));
    }

    @Override
    public ResponseEntity<ConnectionResponse> updateConnection(Long connectionId, UpdateConnectionRequest request) {
        if (request.getName() == null
                && request.getAddress() == null
                && request.getCredentials() == null
                && request.getTls() == null) {
            throw new ErrorResponseException(
                    HttpStatus.BAD_REQUEST,
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.BAD_REQUEST, "Give at least one of name, address, credentials or tls"),
                    null);
        }
        var result = updateConnection.execute(new UpdateConnectionCommand(
                new ConnectionId(connectionId),
                request.getName() == null ? null : new ConnectionName(request.getName()),
                request.getAddress() == null ? null : new BrokerAddress(request.getAddress()),
                request.getCredentials() == null ? null : toCredentials(request.getCredentials()),
                request.getTls() == null ? null : toTls(request.getTls())));
        return ResponseEntity.ok(toResponse(result));
    }

    @Override
    public ResponseEntity<Void> deleteConnection(Long connectionId) {
        deleteConnection.execute(new DeleteConnectionCommand(new ConnectionId(connectionId)));
        return ResponseEntity.noContent().build();
    }

    private static com.oriolvc.topicman.shared.domain.BrokerType toBrokerType(BrokerType brokerType) {
        return com.oriolvc.topicman.shared.domain.BrokerType.valueOf(brokerType.getValue());
    }

    private static Credentials toCredentials(CredentialsRequest request) {
        return switch (request) {
            case NoCredentialsRequest none -> Credentials.none();
            case TokenCredentialsRequest token -> Credentials.token(new Secret(token.getToken()));
            case UsernamePasswordCredentialsRequest usernamePassword -> {
                var username = usernamePassword.getUsername();
                var password = new Secret(usernamePassword.getPassword());
                var mechanism = usernamePassword.getSaslMechanism();
                yield mechanism == null
                        ? Credentials.usernamePassword(username, password)
                        : Credentials.usernamePassword(
                                username,
                                password,
                                com.oriolvc.topicman.shared.domain.SaslMechanism.valueOf(mechanism.getValue()));
            }
            default ->
                throw new IllegalStateException(
                        "Unmapped credentials type " + request.getClass().getSimpleName());
        };
    }

    private static TlsSettings toTls(com.oriolvc.topicman.connection.infrastructure.web.model.TlsSettings request) {
        var certificates = request.getTrustedCertificates();
        return new TlsSettings(
                Boolean.TRUE.equals(request.getEnabled()),
                certificates == null ? null : new TrustedCertificates(certificates));
    }

    private static ConnectionResponse toResponse(ConnectionResult result) {
        var tls = new com.oriolvc.topicman.connection.infrastructure.web.model.TlsSettings()
                .enabled(result.tlsEnabled())
                .trustedCertificates(result.trustedCertificates());
        var mechanism = result.saslMechanism();
        return new ConnectionResponse(
                        result.id(),
                        result.name(),
                        BrokerType.fromValue(result.brokerType().name()),
                        result.address(),
                        CredentialsType.fromValue(result.credentialsType().name()),
                        tls,
                        result.createdAt().atOffset(ZoneOffset.UTC),
                        result.updatedAt().atOffset(ZoneOffset.UTC))
                .saslMechanism(mechanism == null ? null : SaslMechanism.fromValue(mechanism.name()));
    }
}
