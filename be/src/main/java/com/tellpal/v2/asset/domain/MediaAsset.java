package com.tellpal.v2.asset.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Aggregate root for a stored media object and its mutable delivery metadata.
 *
 * <p>The storage location and asset kind are stable identity, while metadata and cached signed
 * download URLs can be refreshed over time.
 */
@Entity
@Table(name = "media_assets")
public class MediaAsset extends BaseJpaEntity {

    private static final Pattern SHA_256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private StorageProvider provider;

    @Column(name = "object_path", nullable = false, length = 1024)
    private String objectPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    private MediaAssetType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 50)
    private MediaKind kind;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "cached_download_url")
    private String cachedDownloadUrl;

    @Column(name = "download_url_cached_at")
    private Instant downloadUrlCachedAt;

    @Column(name = "download_url_expires_at")
    private Instant downloadUrlExpiresAt;

    protected MediaAsset() {
    }

    private MediaAsset(StorageProvider provider, String objectPath, MediaKind kind) {
        this.provider = requireProvider(provider);
        this.objectPath = requireText(objectPath, "Object path must not be blank");
        this.kind = requireKind(kind);
        this.mediaType = kind.getMediaType();
    }

    public static MediaAsset register(StorageProvider provider, String objectPath, MediaKind kind) {
        return new MediaAsset(provider, objectPath, kind);
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public MediaAssetType getMediaType() {
        return mediaType;
    }

    public MediaKind getKind() {
        return kind;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getByteSize() {
        return byteSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getCachedDownloadUrl() {
        return cachedDownloadUrl;
    }

    public Instant getDownloadUrlCachedAt() {
        return downloadUrlCachedAt;
    }

    public Instant getDownloadUrlExpiresAt() {
        return downloadUrlExpiresAt;
    }

    /**
     * Updates mutable metadata discovered for the stored object.
     */
    public void updateMetadata(String mimeType, Long byteSize, String checksumSha256) {
        this.mimeType = normalizeOptionalText(mimeType);
        this.byteSize = normalizeByteSize(byteSize);
        this.checksumSha256 = normalizeChecksum(checksumSha256);
    }

    /**
     * Replaces the cached signed download URL for this asset.
     *
     * <p>The expiry must be after the cache timestamp so callers never persist an already-expired
     * URL.
     */
    public void updateDownloadUrlCache(String downloadUrl, Instant cachedAt, Instant expiresAt) {
        this.cachedDownloadUrl = requireText(downloadUrl, "Cached download URL must not be blank");
        this.downloadUrlCachedAt = requireInstant(cachedAt, "Download URL cached timestamp must not be null");
        this.downloadUrlExpiresAt = requireInstant(expiresAt, "Download URL expiry timestamp must not be null");
        if (!downloadUrlExpiresAt.isAfter(downloadUrlCachedAt)) {
            throw new IllegalArgumentException("Download URL expiry must be after cache timestamp");
        }
    }

    public void clearDownloadUrlCache() {
        cachedDownloadUrl = null;
        downloadUrlCachedAt = null;
        downloadUrlExpiresAt = null;
    }

    public boolean hasActiveDownloadUrlAt(Instant instant) {
        Instant referenceInstant = requireInstant(instant, "Instant must not be null");
        return cachedDownloadUrl != null
                && downloadUrlCachedAt != null
                && downloadUrlExpiresAt != null
                && downloadUrlExpiresAt.isAfter(referenceInstant);
    }

    private static StorageProvider requireProvider(StorageProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Storage provider must not be null");
        }
        return provider;
    }

    private static MediaKind requireKind(MediaKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Media kind must not be null");
        }
        return kind;
    }

    private static Instant requireInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long normalizeByteSize(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("Byte size must not be negative");
        }
        return value;
    }

    private static String normalizeChecksum(String value) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            return null;
        }
        String canonical = normalized.toLowerCase(Locale.ROOT);
        if (!SHA_256_PATTERN.matcher(canonical).matches()) {
            throw new IllegalArgumentException("Checksum must be a lowercase SHA-256 hex string");
        }
        return canonical;
    }
}
