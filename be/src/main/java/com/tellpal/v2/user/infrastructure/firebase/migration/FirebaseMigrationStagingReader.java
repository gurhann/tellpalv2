package com.tellpal.v2.user.infrastructure.firebase.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class FirebaseMigrationStagingReader {

    private final ObjectMapper objectMapper;

    public FirebaseMigrationStagingReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> List<T> readList(Path path, Class<T> elementType) {
        requirePath(path);
        if (elementType == null) {
            throw new IllegalArgumentException("Element type must not be null");
        }
        try {
            JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return objectMapper.readValue(path.toFile(), listType);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Firebase staging file: " + path, exception);
        }
    }

    public long countEntries(Path path) {
        requirePath(path);
        try {
            if (!Files.exists(path)) {
                return 0;
            }
            return objectMapper.readTree(path.toFile()).size();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect Firebase staging file: " + path, exception);
        }
    }

    public boolean exists(Path path) {
        requirePath(path);
        return Files.exists(path);
    }

    private static Path requirePath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Staging path must not be null");
        }
        return path;
    }
}
