package com.tellpal.v2.shared.domain;

import java.time.OffsetDateTime;

public class MediaAsset {

    private final Long id;
    private final String provider;
    private final String objectPath;
    private final MediaKind kind;
    private final String mimeType;
    private final Long bytes;
    private final String checksumSha256;
    private final String downloadUrl;
    private final OffsetDateTime createdAt;

    public MediaAsset(Long id, String provider, String objectPath, MediaKind kind,
                      String mimeType, Long bytes, String checksumSha256,
                      String downloadUrl, OffsetDateTime createdAt) {
        this.id = id;
        this.provider = provider;
        this.objectPath = objectPath;
        this.kind = kind;
        this.mimeType = mimeType;
        this.bytes = bytes;
        this.checksumSha256 = checksumSha256;
        this.downloadUrl = downloadUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getObjectPath() {
        return objectPath;
    }

    public MediaKind getKind() {
        return kind;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getBytes() {
        return bytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
