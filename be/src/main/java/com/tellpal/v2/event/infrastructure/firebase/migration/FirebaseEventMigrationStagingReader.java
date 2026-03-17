package com.tellpal.v2.event.infrastructure.firebase.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FirebaseEventMigrationStagingReader {

    private final ObjectMapper objectMapper;

    public FirebaseEventMigrationStagingReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> List<T> readList(Path path, Class<T> elementType) {
        if (path == null) {
            throw new IllegalArgumentException("Staging path must not be null");
        }
        if (elementType == null) {
            throw new IllegalArgumentException("Element type must not be null");
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(path.toFile(), listType);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Firebase event staging file: " + path, exception);
        }
    }
}
