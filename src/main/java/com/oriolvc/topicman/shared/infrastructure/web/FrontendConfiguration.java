package com.oriolvc.topicman.shared.infrastructure.web;

import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the React app bundled into {@code static/}. The app routes in the browser, so a path that
 * is neither a file nor an API call, such as {@code /connections}, gets {@code index.html} and the
 * app shows the matching screen: every screen has a URL that can be bookmarked or reloaded.
 */
@Configuration(proxyBeanMethods = false)
class FrontendConfiguration implements WebMvcConfigurer {

    private static final String INDEX = "index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new ClientRouteResourceResolver());
    }

    private static final class ClientRouteResourceResolver extends PathResourceResolver {

        @Override
        protected @Nullable Resource getResource(String resourcePath, Resource location) throws IOException {
            var resource = super.getResource(resourcePath, location);
            if (resource != null || !isClientRoute(resourcePath)) {
                return resource;
            }
            return super.getResource(INDEX, location);
        }

        /** Unknown API paths and missing files stay 404s rather than turning into the app. */
        private static boolean isClientRoute(String resourcePath) {
            var lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            return !resourcePath.startsWith("api/") && !lastSegment.contains(".");
        }
    }
}
