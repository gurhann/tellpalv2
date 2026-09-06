package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/** Optional full-length audio narration owned by one story localization. */
@Entity
@Table(name = "story_narrations")
public class StoryNarration extends BaseJpaEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_localization_id", nullable = false, unique = true)
    private ContentLocalization localization;

    @Column(name = "audio_media_id", nullable = false)
    private Long audioMediaId;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    protected StoryNarration() { }

    StoryNarration(ContentLocalization localization, Long audioMediaId, Integer durationMinutes) {
        this.localization = requireLocalization(localization);
        update(audioMediaId, durationMinutes);
    }

    public Long getAudioMediaId() { return audioMediaId; }

    public Integer getDurationMinutes() { return durationMinutes; }

    void update(Long audioMediaId, Integer durationMinutes) {
        if (audioMediaId == null || audioMediaId <= 0) {
            throw new IllegalArgumentException("Narration audio media ID must be positive");
        }
        if (durationMinutes == null || durationMinutes < 0) {
            throw new IllegalArgumentException("Narration duration minutes must be non-negative");
        }
        this.audioMediaId = audioMediaId;
        this.durationMinutes = durationMinutes;
    }

    private static ContentLocalization requireLocalization(ContentLocalization localization) {
        if (localization == null) {
            throw new IllegalArgumentException("Story narration localization must not be null");
        }
        return localization;
    }
}
