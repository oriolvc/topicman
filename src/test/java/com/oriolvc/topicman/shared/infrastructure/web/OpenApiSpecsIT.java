package com.oriolvc.topicman.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class OpenApiSpecsIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final MockMvcTester mvc;

    OpenApiSpecsIT(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void servesTheHandWrittenSpecs() {
        assertThat(mvc.get().uri("/openapi/common.yaml"))
                .hasStatusOk()
                .bodyText()
                .contains("ProblemDetail:");
    }

    @Test
    void doesNotGenerateASpecFromTheControllers() {
        assertThat(mvc.get().uri("/v3/api-docs"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.paths")
                .asMap()
                .isEmpty();
    }

    @Test
    void servesSwaggerUi() {
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
    }

    @Test
    void listsEveryContextSpecInSwaggerUi() throws IOException {
        var contextSpecs = Arrays.stream(
                        new PathMatchingResourcePatternResolver().getResources("classpath:/openapi/*.yaml"))
                .map(resource -> Objects.requireNonNull(resource.getFilename()))
                .filter(name -> !name.equals("common.yaml"))
                .map(name -> "/openapi/" + name)
                .toList();

        var swaggerConfig = mvc.get().uri("/v3/api-docs/swagger-config").exchange();

        assertThat(swaggerConfig).hasStatusOk();
        var urls = JSON.readTree(swaggerConfig.getResponse().getContentAsString())
                .path("urls")
                .valueStream()
                .map(url -> url.path("url").asString())
                .toList();
        assertThat(urls).containsExactlyInAnyOrderElementsOf(contextSpecs);
    }
}
