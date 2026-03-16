package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StoryPageLocalizationId implements Serializable {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    protected StoryPageLocalizationId() {
    }

    public StoryPageLocalizationId(Long contentId, int pageNumber, String languageCode) {
        this.contentId = contentId;
        this.pageNumber = pageNumber;
        this.languageCode = languageCode;
    }

    public Long getContentId() {
        return contentId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoryPageLocalizationId that)) return false;
        return pageNumber == that.pageNumber
            && Objects.equals(contentId, that.contentId)
            && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, pageNumber, languageCode);
    }
}
