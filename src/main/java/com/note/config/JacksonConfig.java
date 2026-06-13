package com.note.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 处理 LocalDateTime 转换格式
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    };

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                    String value = p.getText();
                    if (value == null || value.isEmpty()) {
                        return null;
                    }
                    if (value.length() <= 10) {
                        return LocalDateTime.parse(value + " 00:00:00", FORMATTERS[0]);
                    }
                    // replace T separator for uniform parsing
                    String normalized = value.replace('T', ' ');
                    for (DateTimeFormatter formatter : FORMATTERS) {
                        try {
                            return LocalDateTime.parse(normalized, formatter);
                        } catch (DateTimeParseException ignored) {
                        }
                    }
                    throw new IOException("Cannot parse LocalDateTime: " + value);
                }
            });
        };
    }
}
