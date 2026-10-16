package com.oriolvc.topicman;

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
