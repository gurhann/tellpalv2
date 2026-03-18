package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * Grant that marks one content localization free for a specific access key.
 */
@Entity
@Table(name = "content_free_access")
public class ContentFreeAccess extends BaseJpaEntity {

    @Column(name = "access_key", nullable = false, length = 120)
    private String accessKey;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 8)
    private LanguageCode languageCode;

    protected ContentFreeAccess() {
    }

    private ContentFreeAccess(String accessKey, Long contentId, LanguageCode languageCode) {
        this.accessKey = requireAccessKey(accessKey);
        this.contentId = requireContentId(contentId);
        this.languageCode = requireLanguageCode(languageCode);
    }

    /**
     * Creates a free-access grant for one content item and language.
     */
    public static ContentFreeAccess grant(String accessKey, Long contentId, LanguageCode languageCode) {
        return new ContentFreeAccess(accessKey, contentId, languageCode);
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Long getContentId() {
        return contentId;
    }

    public LanguageCode getLanguageCode() {
        return languageCode;
    }

    private static String requireAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalArgumentException("Access key must not be blank");
        }
        return accessKey.trim();
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }
}
