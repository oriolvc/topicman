package com.oriolvc.topicman.shared.infrastructure.examples;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestCertificates;
import com.oriolvc.topicman.TestcontainersConfiguration;
import com.oriolvc.topicman.collection.application.model.GetCollectionCommand;
import com.oriolvc.topicman.collection.application.model.ListCollectionsCommand;
import com.oriolvc.topicman.collection.application.model.ListEnvironmentsCommand;
import com.oriolvc.topicman.collection.application.model.ResolveTemplateCommand;
import com.oriolvc.topicman.collection.application.usecase.GetCollectionUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListCollectionsUseCase;
import com.oriolvc.topicman.collection.application.usecase.ListEnvironmentsUseCase;
import com.oriolvc.topicman.collection.application.usecase.ResolveTemplateUseCase;
import com.oriolvc.topicman.collection.domain.model.CollectionId;
import com.oriolvc.topicman.collection.domain.model.EnvironmentId;
import com.oriolvc.topicman.collection.domain.model.TemplateId;
import com.oriolvc.topicman.connection.application.model.BrokerCredentials;
import com.oriolvc.topicman.connection.application.model.ConnectionResult;
import com.oriolvc.topicman.connection.application.model.GetBrokerAccessCommand;
import com.oriolvc.topicman.connection.application.model.ListConnectionsCommand;
import com.oriolvc.topicman.connection.application.usecase.GetBrokerAccessUseCase;
import com.oriolvc.topicman.connection.application.usecase.ListConnectionsUseCase;
import com.oriolvc.topicman.shared.domain.BrokerType;
import com.oriolvc.topicman.shared.domain.ConnectionId;
import com.oriolvc.topicman.shared.domain.SaslMechanism;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;

/** Tests share one context and so one load: each states only what it needs of the data. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "topicman.seed-examples=true")
@TestConstructor(autowireMode = AutowireMode.ALL)
class ExampleDataIT {

    private final ListConnectionsUseCase connections;
    private final GetBrokerAccessUseCase brokerAccess;
    private final ListCollectionsUseCase collections;
    private final GetCollectionUseCase collection;
    private final ListEnvironmentsUseCase environments;
    private final ResolveTemplateUseCase resolve;
    private final ExampleDataInitializer initializer;
    private final JdbcClient jdbc;

    ExampleDataIT(
            ListConnectionsUseCase connections,
            GetBrokerAccessUseCase brokerAccess,
            ListCollectionsUseCase collections,
            GetCollectionUseCase collection,
            ListEnvironmentsUseCase environments,
            ResolveTemplateUseCase resolve,
            ExampleDataInitializer initializer,
            JdbcClient jdbc) {
        this.connections = connections;
        this.brokerAccess = brokerAccess;
        this.collections = collections;
        this.collection = collection;
        this.environments = environments;
        this.resolve = resolve;
        this.initializer = initializer;
        this.jdbc = jdbc;
    }

    private List<ConnectionResult> savedConnections() {
        return connections.execute(new ListConnectionsCommand());
    }

    private BrokerCredentials credentialsOf(ConnectionResult connection) {
        return brokerAccess
                .execute(new GetBrokerAccessCommand(new ConnectionId(connection.id())))
                .credentials();
    }

    @Test
    void createsAConnectionWithEachKindOfCredentialsForEachBroker() {
        var saved = savedConnections();

        assertThat(saved).extracting(ConnectionResult::brokerType).contains(BrokerType.values());
        assertThat(saved).extracting(this::credentialsOf).hasAtLeastOneElementOfType(BrokerCredentials.Token.class);
        assertThat(saved)
                .extracting(ConnectionResult::saslMechanism)
                .contains(SaslMechanism.PLAIN, SaslMechanism.SCRAM_SHA_256, SaslMechanism.SCRAM_SHA_512);
        assertThat(saved)
                .filteredOn(connection -> connection.brokerType() == BrokerType.RABBITMQ)
                .extracting(this::credentialsOf)
                .hasAtLeastOneElementOfType(BrokerCredentials.UsernamePassword.class)
                .contains(BrokerCredentials.none());
    }

    @Test
    void securedConnectionsTrustTheDemoCertificate() {
        var secured = savedConnections().stream()
                .filter(connection -> connection.trustedCertificates() != null)
                .toList();

        assertThat(secured).hasSizeGreaterThanOrEqualTo(3);
        assertThat(secured)
                .extracting(ConnectionResult::trustedCertificates)
                .allSatisfy(pem -> assertThat(pem).isEqualToIgnoringWhitespace(TestCertificates.CA));
    }

    @Test
    void createsJsonAndTextTemplatesForEachBrokerThatResolveInEveryEnvironment() {
        var environmentIds = environments.execute(new ListEnvironmentsCommand()).stream()
                .map(environment -> environment.id())
                .toList();
        assertThat(environmentIds).hasSizeGreaterThanOrEqualTo(2);
        var templates = collections.execute(new ListCollectionsCommand()).stream()
                .map(summary -> collection.execute(new GetCollectionCommand(new CollectionId(summary.id()))))
                .flatMap(result -> result.templates().stream()
                        .map(template -> new SavedTemplate(result.id(), template.id(), template.brokerType())))
                .toList();

        assertThat(templates).extracting(SavedTemplate::brokerType).contains(BrokerType.values());
        var formats = new java.util.HashSet<String>();
        for (var template : templates) {
            for (var environmentId : environmentIds) {
                var message = resolve.execute(new ResolveTemplateCommand(
                        new CollectionId(template.collectionId()),
                        new TemplateId(template.id()),
                        new EnvironmentId(environmentId)));

                assertThat(message.toString()).doesNotContain("{{");
                formats.add(message.payload().format().name());
            }
        }
        assertThat(formats).containsExactlyInAnyOrder("JSON", "TEXT");
    }

    @Test
    void loadsOnlyOnceSoDeletedExamplesDoNotComeBack() {
        var before = savedConnections().size();

        initializer.run(null);

        assertThat(savedConnections()).hasSize(before);
    }

    @Test
    void marksADatabaseThatAlreadyHasDataWithoutAddingToIt() {
        jdbc.sql("DELETE FROM example_data_load").update();
        var before = savedConnections().size();

        initializer.run(null);

        assertThat(savedConnections()).hasSize(before);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM example_data_load")
                        .query(Long.class)
                        .single())
                .isEqualTo(1);
    }

    private record SavedTemplate(long collectionId, long id, BrokerType brokerType) {}
}
