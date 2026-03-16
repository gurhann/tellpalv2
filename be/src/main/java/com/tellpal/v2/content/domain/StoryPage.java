package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "v2_story_pages")
public class StoryPage {

    @EmbeddedId
    private StoryPageId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("contentId")
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "illustration_media_id")
    private Long illustrationMediaId;

    protected StoryPage() {
    }

    public StoryPage(Content content, int pageNumber) {
        this.content = content;
        this.id = new StoryPageId(content.getId(), pageNumber);
    }

    public StoryPageId getId() {
        return id;
    }

    public Content getContent() {
        return content;
    }

    public int getPageNumber() {
        return id.getPageNumber();
    }

    public Long getIllustrationMediaId() {
        return illustrationMediaId;
    }

    public void setIllustrationMediaId(Long illustrationMediaId) {
        this.illustrationMediaId = illustrationMediaId;
    }
}
