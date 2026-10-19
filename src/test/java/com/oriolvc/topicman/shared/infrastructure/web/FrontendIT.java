package com.oriolvc.topicman.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class FrontendIT {

    private static final String INDEX = "index.html";
    private static final String APP_ROOT = "<div id=\"root\"></div>";

    private final MockMvcTester mvc;

    FrontendIT(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void servesTheBundledFrontend() {
        assertThat(mvc.get().uri("/" + INDEX)).hasStatusOk().bodyText().contains(APP_ROOT);
    }

    @Test
    void servesTheFrontendAsTheWelcomePage() {
        assertThat(mvc.get().uri("/")).hasStatusOk().hasForwardedUrl(INDEX);
    }

    @Test
    void servesTheFrontendForClientRoutes() {
        assertThat(mvc.get().uri("/connections")).hasStatusOk().bodyText().contains(APP_ROOT);
        assertThat(mvc.get().uri("/consume/sessions/42"))
                .hasStatusOk()
                .bodyText()
                .contains(APP_ROOT);
    }

    @Test
    void keepsMissingFilesNotFound() {
        assertThat(mvc.get().uri("/assets/missing.js")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void keepsUnknownApiPathsNotFound() {
        assertThat(mvc.get().uri("/api/v1/unknown")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void stillServesTheApi() {
        // Other tests share the database, so only the kind of response is checked.
        assertThat(mvc.get().uri("/api/v1/connections")).hasStatusOk().hasContentType(MediaType.APPLICATION_JSON);
    }
}
