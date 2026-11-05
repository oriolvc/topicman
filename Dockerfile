# TopicMan, with nothing else to configure (README.md, "Run with Docker"). Publishing happens in
# .github/workflows/release.yml.

# The jar is the same for every platform, so it is built once, without emulating the target's.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
# Gradle downloads its own Node.js to build the UI into the jar.
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon --console=plain
# Layers, so a new version only replaces the application's: dependencies change less often.
RUN find build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} topicman.jar \; \
    && java -Djarmode=tools -jar topicman.jar extract --layers --destination /layers \
    && mkdir /data

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /layers/dependencies/ ./
COPY --from=build /layers/spring-boot-loader/ ./
COPY --from=build /layers/snapshot-dependencies/ ./
COPY --from=build /layers/application/ ./
# A numeric user: the image has no entry for it, and needs none. New named volumes take the owner of
# this directory, so the embedded database and generated master key can be written.
COPY --from=build --chown=1000:1000 /data /data
USER 1000:1000
# Inside a container, the default loopback address cannot be reached through a published port. Who
# can reach TopicMan is decided where the port is published: see the README.
ENV SERVER_ADDRESS=0.0.0.0
ENV TOPICMAN_DATA_DIR=/data
VOLUME /data
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "topicman.jar"]
