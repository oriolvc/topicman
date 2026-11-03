package com.oriolvc.topicman.shared.infrastructure.web;

import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Reads and writes {@code application/yaml}, for the files collections and environments are
 * exported to (ADR 0006). The files are meant to be read and diffed in Git, so they are written
 * without a document marker, with quotes only where YAML needs them, and multi-line texts as
 * literal blocks. A key given twice is an error rather than silently losing a value.
 */
@Configuration(proxyBeanMethods = false)
class YamlConfiguration {

    @Bean
    ServerHttpMessageConvertersCustomizer yamlHttpMessageConverter() {
        var mapper = YAMLMapper.builder()
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                // Type ids as properties (`type: TOPIC`), as in JSON, rather than YAML tags.
                .disable(YAMLWriteFeature.USE_NATIVE_TYPE_ID)
                // Properties in the spec's order: Jackson 3 sorts them alphabetically, with the
                // generated constructors' parameters first, by default.
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(MapperFeature.SORT_CREATOR_PROPERTIES_FIRST)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
                .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
                .enable(YAMLWriteFeature.INDENT_ARRAYS_WITH_INDICATOR)
                .disable(YAMLWriteFeature.SPLIT_LINES)
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .build();
        return converters -> converters.withYamlConverter(new JacksonYamlHttpMessageConverter(mapper));
    }
}
