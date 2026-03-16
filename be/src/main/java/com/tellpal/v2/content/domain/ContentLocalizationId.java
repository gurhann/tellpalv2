package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ContentLocalizationId implements Serializable {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    protected ContentLocalizationId() {
    }

    public ContentLocalizationId(Long contentId, String languageCode) {
        this.contentId = contentId;
        this.languageCode = languageCode;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContentLocalizationId that)) return false;
        return Objects.equals(contentId, that.contentId)
            && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, languageCode);
    }
}
