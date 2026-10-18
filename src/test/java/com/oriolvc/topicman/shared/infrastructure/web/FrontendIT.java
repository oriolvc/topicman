package com.oriolvc.topicman.shared.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestConstructor(autowireMode = AutowireMode.ALL)
class FrontendIT {

    private final MockMvcTester mvc;

    FrontendIT(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    void servesTheBundledFrontend() {
        assertThat(mvc.get().uri("/index.html")).hasStatusOk().bodyText().contains("<div id=\"root\"></div>");
    }

    @Test
    void servesTheFrontendAsTheWelcomePage() {
        assertThat(mvc.get().uri("/")).hasStatusOk().hasForwardedUrl("index.html");
    }
}
