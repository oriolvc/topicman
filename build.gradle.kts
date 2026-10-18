import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.openapi.generator)
}

group = "com.oriolvc"
version = "0.0.1-SNAPSHOT"
description = "topicman"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.kafka.clients)
    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.flyway.test)
    testImplementation(libs.spring.boot.starter.validation.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// `spotlessCheck` runs as part of `./gradlew check`; fix violations with `./gradlew spotlessApply`.
spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat(libs.versions.palantir.java.format.get())
        removeUnusedImports()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// API first (ADR 0004): each bounded context's hand-written spec generates its controller
// interfaces and DTOs. The output is build-only: never committed, never formatted by Spotless.
val openApiSpecs = layout.projectDirectory.dir("src/main/resources/openapi")
val openApiOutput = layout.buildDirectory.dir("generated/openapi")
val openApiContexts = listOf("connection", "messaging", "collection")
    .filter { openApiSpecs.file("$it.yaml").asFile.exists() }

val openApiGenerateTasks = openApiContexts.map { context ->
    tasks.register<GenerateTask>("openApiGenerate${context.replaceFirstChar(Char::uppercase)}") {
        group = "openapi"
        description = "Generates the $context API interfaces and DTOs from $context.yaml."
        generatorName = "spring"
        library = "spring-boot"
        inputSpec = openApiSpecs.file("$context.yaml")
        // Specs reference common.yaml, so any spec change must regenerate.
        inputs.dir(openApiSpecs)
        outputDir = openApiOutput.map { it.dir(context) }
        cleanupOutput = true
        apiPackage = "com.oriolvc.topicman.$context.infrastructure.web.api"
        modelPackage = "com.oriolvc.topicman.$context.infrastructure.web.model"
        // Errors are Spring's ProblemDetail, mapped from domain error codes; never a generated DTO.
        schemaMappings = mapOf("ProblemDetail" to "org.springframework.http.ProblemDetail")
        generateApiTests = false
        generateModelTests = false
        generateApiDocumentation = false
        generateModelDocumentation = false
        configOptions = mapOf(
            "useSpringBoot4" to "true",
            "useJackson3" to "true",
            "useJakartaEe" to "true",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "useBeanValidation" to "true",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "hideGenerationTimestamp" to "true",
            "sourceFolder" to "src/main/java",
        )
    }
}

sourceSets.main {
    java.srcDirs(openApiContexts.map { openApiOutput.map { output -> output.dir("$it/src/main/java") } })
}

tasks.compileJava {
    dependsOn(openApiGenerateTasks)
}
