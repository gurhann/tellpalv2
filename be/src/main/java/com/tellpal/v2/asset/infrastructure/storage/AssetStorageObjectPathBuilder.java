package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetKind;

/**
 * Builds Firebase object paths under the configured environment prefix.
 */
@Component
public class AssetStorageObjectPathBuilder {

    private final String pathPrefix;

    public AssetStorageObjectPathBuilder(AssetStorageFirebaseProperties properties) {
        this.pathPrefix = normalizePathPrefix(properties.pathPrefix());
    }

    public String manualUploadPath(AssetKind kind, String fileName, Instant instant) {
        ZonedDateTime timestamp = requireInstant(instant).atZone(ZoneOffset.UTC);
        String sanitizedFileName = sanitizeFileName(fileName);
        String manualFolder = switch (requireUploadKind(kind)) {
            case ORIGINAL_IMAGE -> "images";
            case ORIGINAL_AUDIO -> "audio";
            default -> throw new IllegalArgumentException("Manual upload kind is not supported: " + kind);
        };
        return "/%s/manual/%s/original/%04d/%02d/%s-%s".formatted(
                pathPrefix,
                manualFolder,
                timestamp.getYear(),
                timestamp.getMonthValue(),
                UUID.randomUUID(),
                sanitizedFileName);
    }

    public String prefixPath(String relativePath) {
        String normalizedRelativePath = normalizeRelativePath(relativePath);
        return "/" + pathPrefix + "/" + normalizedRelativePath;
    }

    public String requireManagedFirebasePath(String objectPath) {
        String normalizedPath = normalizeAbsolutePath(objectPath);
        String requiredPrefix = "/" + pathPrefix + "/";
        if (!normalizedPath.startsWith(requiredPrefix)) {
            throw new IllegalArgumentException("Firebase object path must stay inside the configured path prefix");
        }
        return normalizedPath;
    }

    public String pathPrefix() {
        return pathPrefix;
    }

    private static AssetKind requireUploadKind(AssetKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Manual upload asset kind must not be null");
        }
        return kind;
    }

    private static Instant requireInstant(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("Upload timestamp must not be null");
        }
        return instant;
    }

    private static String normalizePathPrefix(String pathPrefix) {
        if (pathPrefix == null || pathPrefix.isBlank()) {
            throw new IllegalArgumentException("Firebase storage path prefix must not be blank");
        }
        String normalized = pathPrefix.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("/") || normalized.contains("\\")) {
            throw new IllegalArgumentException("Firebase storage path prefix must not contain path separators");
        }
        return normalized;
    }

    private static String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative storage path must not be blank");
        }
        String normalized = relativePath.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Relative storage path must not be blank");
        }
        return normalized;
    }

    private static String normalizeAbsolutePath(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            throw new IllegalArgumentException("Firebase object path must not be blank");
        }
        String normalized = objectPath.trim();
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Upload file name must not be blank");
        }
        String normalized = fileName.trim().replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        normalized = normalized
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[.-]+", "")
                .replaceAll("[.-]+$", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Upload file name must contain at least one safe character");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
