package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StoryPageId implements Serializable {

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    protected StoryPageId() {
    }

    public StoryPageId(Long contentId, int pageNumber) {
        this.contentId = contentId;
        this.pageNumber = pageNumber;
    }

    public Long getContentId() {
        return contentId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StoryPageId that)) return false;
        return pageNumber == that.pageNumber && Objects.equals(contentId, that.contentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentId, pageNumber);
    }
}
