package com.tellpal.v2.asset.infrastructure.storage;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.shared.domain.LanguageCode;

@Component
public class AssetProcessingPathBuilder {

    private final AssetStorageObjectPathBuilder assetStorageObjectPathBuilder;

    public AssetProcessingPathBuilder(AssetStorageObjectPathBuilder assetStorageObjectPathBuilder) {
        this.assetStorageObjectPathBuilder = assetStorageObjectPathBuilder;
    }

    public String originalRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "original");
    }

    public String processedRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "processed");
    }

    public String packagesRoot(String contentType, String externalKey, LanguageCode languageCode) {
        return buildRoot(contentType, externalKey, languageCode, "packages");
    }

    public String coverVariantPath(AssetProcessingRecord assetProcessingRecord, AssetKind assetKind) {
        String root = processedRoot(
                assetProcessingRecord.contentType().name(),
                assetProcessingRecord.externalKey(),
                assetProcessingRecord.languageCode());
        return switch (assetKind) {
            case THUMBNAIL_PHONE -> root + "cover-thumbnail-phone.webp";
            case THUMBNAIL_TABLET -> root + "cover-thumbnail-tablet.webp";
            case DETAIL_PHONE -> root + "cover-detail-phone.webp";
            case DETAIL_TABLET -> root + "cover-detail-tablet.webp";
            default -> throw new IllegalArgumentException("Unsupported cover variant kind: " + assetKind);
        };
    }

    public String optimizedAudioPath(AssetProcessingRecord assetProcessingRecord) {
        return processedRoot(
                assetProcessingRecord.contentType().name(),
                assetProcessingRecord.externalKey(),
                assetProcessingRecord.languageCode()) + "audio-optimized.m4a";
    }

    public String packagePath(AssetProcessingRecord assetProcessingRecord, AssetKind assetKind) {
        String root = packagesRoot(
                assetProcessingRecord.contentType().name(),
                assetProcessingRecord.externalKey(),
                assetProcessingRecord.languageCode());
        return switch (assetKind) {
            case CONTENT_ZIP -> root + assetProcessingRecord.externalKey() + ".zip";
            case CONTENT_ZIP_PART1 -> root + assetProcessingRecord.externalKey() + "_part1.zip";
            case CONTENT_ZIP_PART2 -> root + assetProcessingRecord.externalKey() + "_part2.zip";
            default -> throw new IllegalArgumentException("Unsupported package kind: " + assetKind);
        };
    }

    private String buildRoot(String contentType, String externalKey, LanguageCode languageCode, String folder) {
        return assetStorageObjectPathBuilder.prefixPath("content/%s/%s/%s/%s/".formatted(
                normalizeContentType(contentType),
                normalizeExternalKey(externalKey),
                requireLanguageCode(languageCode).value(),
                folder));
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
