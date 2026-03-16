package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "v2_story_page_localizations")
public class StoryPageLocalization {

    @EmbeddedId
    private StoryPageLocalizationId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumns({
        @JoinColumn(name = "content_id", referencedColumnName = "content_id"),
        @JoinColumn(name = "page_number", referencedColumnName = "page_number")
    })
    private StoryPage storyPage;

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "audio_media_id")
    private Long audioMediaId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected StoryPageLocalization() {
    }

    public StoryPageLocalization(StoryPage storyPage, String languageCode) {
        this.storyPage = storyPage;
        this.id = new StoryPageLocalizationId(
            storyPage.getId().getContentId(),
            storyPage.getPageNumber(),
            languageCode
        );
    }

    public StoryPageLocalizationId getId() {
        return id;
    }

    public StoryPage getStoryPage() {
        return storyPage;
    }

    public String getLanguageCode() {
        return id.getLanguageCode();
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public Long getAudioMediaId() {
        return audioMediaId;
    }

    public void setAudioMediaId(Long audioMediaId) {
        this.audioMediaId = audioMediaId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
