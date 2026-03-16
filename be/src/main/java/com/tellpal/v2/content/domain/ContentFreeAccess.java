package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_content_free_access")
public class ContentFreeAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_key", nullable = false)
    private String accessKey;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ContentFreeAccess() {
    }

    public ContentFreeAccess(String accessKey, Long contentId, String languageCode) {
        this.accessKey = accessKey;
        this.contentId = contentId;
        this.languageCode = languageCode;
    }

    public Long getId() {
        return id;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
