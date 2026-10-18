package com.oriolvc.topicman.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the hand-written OpenAPI specs, the source of truth for the API (ADR 0004), so Swagger UI
 * and API clients read exactly what was reviewed. springdoc does not generate a spec from code.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiSpecsConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**").addResourceLocations("classpath:/openapi/");
    }
}
