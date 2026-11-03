import com.github.gradle.node.npm.task.NpmTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.node.gradle)
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
    implementation(libs.pulsar.client)
    implementation(libs.amqp.client)
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
    testImplementation(libs.testcontainers.pulsar)
    testImplementation(libs.testcontainers.rabbitmq)
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
        // Each context needs its own, or their generated configuration classes would clash.
        configOptions.put("configPackage", "com.oriolvc.topicman.$context.infrastructure.web.config")
        // Errors are Spring's ProblemDetail, mapped from domain error codes; never a generated DTO.
        schemaMappings = mapOf("ProblemDetail" to "org.springframework.http.ProblemDetail")
        // `format: event-stream` marks a server-sent event stream, so its operation returns an
        // SseEmitter and the streaming controller still implements the generated interface.
        typeMappings = mapOf("string+event-stream" to "SseEmitter")
        importMappings = mapOf("SseEmitter" to "org.springframework.web.servlet.mvc.method.annotation.SseEmitter")
        generateApiTests = false
        generateModelTests = false
        generateApiDocumentation = false
        generateModelDocumentation = false
        configOptions.putAll(mapOf(
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
            // The specs leave optional properties out rather than set them to null.
            "additionalModelTypeAnnotations" to
                "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)",
            "sourceFolder" to "src/main/java",
        ))
    }
}

sourceSets.main {
    java.srcDirs(openApiContexts.map { openApiOutput.map { output -> output.dir("$it/src/main/java") } })
}

tasks.compileJava {
    dependsOn(openApiGenerateTasks)
}

// The React app in frontend/ is built with a Node version Gradle downloads, so building the jar
// needs no local Node. Its output is served by Spring Boot as static resources.
val frontendDir = layout.projectDirectory.dir("frontend")

node {
    download = true
    version = "22.23.2"
    npmInstallCommand = "ci"
    nodeProjectDir = frontendDir
}

// The frontend generates its API types from the specs, so a spec change reruns its tasks.
val frontendSources = listOf("src", "index.html", "package.json", "package-lock.json")
    .map { frontendDir.file(it) } +
    frontendDir.asFileTree.matching { include("*.config.*", "tsconfig.json", ".prettier*", "redocly.yaml") } +
    openApiSpecs.asFileTree

fun registerNpmScript(name: String, script: String, describe: String) =
    tasks.register<NpmTask>(name) {
        group = "frontend"
        description = describe
        dependsOn(tasks.npmInstall)
        args = listOf("run", script)
        inputs.files(frontendSources).withPropertyName("frontendSources")
    }

val frontendBuild = registerNpmScript("frontendBuild", "build", "Builds the frontend into frontend/dist.")
frontendBuild.configure { outputs.dir(frontendDir.dir("dist")) }

// The checks have no outputs; a marker file lets Gradle skip them when nothing changed.
val frontendChecks = listOf(
    registerNpmScript("frontendLint", "lint", "Runs ESLint and the Prettier check on the frontend."),
    registerNpmScript("frontendTypecheck", "typecheck", "Type-checks the frontend."),
    registerNpmScript("frontendTest", "test", "Runs the frontend tests."),
)
frontendChecks.forEach { task ->
    task.configure {
        val marker = layout.buildDirectory.file("frontend-checks/$name")
        outputs.file(marker)
        doLast { marker.get().asFile.writeText("passed") }
    }
}

tasks.processResources {
    from(frontendBuild) { into("static") }
}

tasks.check {
    dependsOn(frontendChecks)
}
