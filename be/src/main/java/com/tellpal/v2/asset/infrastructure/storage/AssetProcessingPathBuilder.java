package com.tellpal.v2.asset.infrastructure.storage;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.tellpal.v2.shared.domain.LanguageCode;

@Component
public class AssetProcessingPathBuilder {

    public String originalRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "original");
    }

    public String processedRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "processed");
    }

    public String packagesRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "packages");
    }

    private String buildRoot(String contentType, String externalKey, LanguageCode languageCode, String folder) {
        return "/content/%s/%s/%s/%s/".formatted(
                normalizeContentType(contentType),
                normalizeExternalKey(externalKey),
                requireLanguageCode(languageCode).value(),
                folder);
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type must not be blank");
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeExternalKey(String externalKey) {
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("External key must not be blank");
        }
        return externalKey.trim();
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
