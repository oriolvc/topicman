package com.oriolvc.topicman;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Enforces the architecture rules from CLAUDE.md and ADR 0002. Never relax a rule to make code
 * pass: change the code, or propose an ADR.
 */
@AnalyzeClasses(packages = "com.oriolvc.topicman", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String BASE = "com.oriolvc.topicman";
    private static final String CONTEXTS = BASE + ".[connection|messaging|collection]";

    @ArchTest
    static final ArchRule layersDependOnlyInward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain")
            .definedBy(BASE + "..domain..")
            .layer("Application")
            .definedBy(BASE + "..application..")
            .layer("Infrastructure")
            .definedBy(BASE + "..infrastructure..")
            .whereLayer("Infrastructure")
            .mayNotBeAccessedByAnyLayer()
            .whereLayer("Application")
            .mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain")
            .mayOnlyBeAccessedByLayers("Application", "Infrastructure");

    @ArchTest
    static final ArchRule domainHasNoFrameworkDependencies = noClasses()
            .that()
            .resideInAPackage(BASE + "..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.transaction..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..",
                    "org.apache.kafka..",
                    "org.apache.pulsar..",
                    "com.rabbitmq..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule connectionInternalsStayInConnection = internalsStayInside("connection");

    @ArchTest
    static final ArchRule messagingInternalsStayInMessaging = internalsStayInside("messaging");

    @ArchTest
    static final ArchRule collectionInternalsStayInCollection = internalsStayInside("collection");

    /**
     * The only use case that returns decrypted credentials is called by messaging's broker endpoint
     * adapter alone, so no controller can return a secret (ADR 0008).
     */
    @ArchTest
    static final ArchRule onlyBrokerEndpointsGetBrokerAccess = noClasses()
            .that()
            .resideOutsideOfPackages(
                    BASE + ".connection.application..", BASE + ".messaging.infrastructure.connection..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameStartingWith("GetBrokerAccess")
            .orShould()
            .dependOnClassesThat()
            .haveFullyQualifiedName(BASE + ".connection.application.model.BrokerAccessResult")
            .orShould()
            .dependOnClassesThat()
            .haveNameMatching(BASE.replace(".", "\\.") + "\\.connection\\.application\\.model\\.BrokerCredentials.*")
            .because("decrypted credentials must never reach a controller or the API (ADR 0008)");

    /** Every class in a context's inner layers belongs to a subpackage for its role (ADR 0009). */
    @ArchTest
    static final ArchRule innerLayersHaveNoLooseClasses =
            noClasses().should().resideInAnyPackage(CONTEXTS + ".domain", CONTEXTS + ".application");

    /** Helpers are public only so use cases can reach them; adapters must not (ADR 0009). */
    @ArchTest
    static final ArchRule helpersStayInTheApplicationLayer = noClasses()
            .that()
            .resideOutsideOfPackage(CONTEXTS + ".application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(CONTEXTS + ".application.helper");

    @ArchTest
    static final ArchRule useCasesLiveInUsecase = classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .should()
            .resideInAPackage(CONTEXTS + ".application.usecase");

    @ArchTest
    static final ArchRule commandsAndResultsLiveInApplicationModel = classes()
            .that()
            .haveSimpleNameEndingWith("Command")
            .or()
            .haveSimpleNameEndingWith("Result")
            .should()
            .resideInAPackage(CONTEXTS + ".application.model");

    @ArchTest
    static final ArchRule domainExceptionsLiveInException = classes()
            .that()
            .resideInAPackage(CONTEXTS + ".domain..")
            .and()
            .areAssignableTo(RuntimeException.class)
            .should()
            .resideInAPackage(CONTEXTS + ".domain.exception");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that()
            .resideInAPackage(CONTEXTS + ".domain.port")
            .should()
            .beInterfaces();

    /**
     * Other contexts may use a context only through its application layer (or domain events, which
     * are published through it), never through its domain or infrastructure packages.
     */
    private static ArchRule internalsStayInside(String context) {
        String contextPackage = BASE + "." + context;
        return noClasses()
                .that()
                .resideOutsideOfPackage(contextPackage + "..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(contextPackage + ".domain..", contextPackage + ".infrastructure..")
                .because("bounded contexts talk through application-layer APIs or domain events")
                .allowEmptyShould(true);
    }
}
