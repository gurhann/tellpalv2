package com.tellpal.v2.shared.infrastructure.persistence;

import com.tellpal.v2.shared.domain.MediaKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_media_assets")
public class MediaAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "object_path", nullable = false)
    private String objectPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 32)
    private MediaKind kind;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "bytes")
    private Long bytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "download_url")
    private String downloadUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MediaAssetEntity() {
    }

    public MediaAssetEntity(String provider, String objectPath, MediaKind kind,
                            String mimeType, Long bytes, String checksumSha256) {
        this.provider = provider;
        this.objectPath = objectPath;
        this.kind = kind;
        this.mimeType = mimeType;
        this.bytes = bytes;
        this.checksumSha256 = checksumSha256;
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

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
