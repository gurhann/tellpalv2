package com.tellpal.v2.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity;

/**
 * The single content-scoped playback source owned by a lullaby.
 *
 * <p>Localization rows deliberately do not own any of this data. The listening cover remains on
 * {@link Content} because it is a shared content-level cover owned by the cover story.
 */
@Entity
@Table(name = "lullaby_playbacks")
public class LullabyPlayback extends BaseJpaEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;

    @Column(name = "audio_media_id", nullable = false)
    private Long audioMediaId;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    protected LullabyPlayback() {
    }

    LullabyPlayback(Content content, Long audioMediaId, Integer durationMinutes) {
        this.content = requireContent(content);
        update(audioMediaId, durationMinutes);
    }

    public Long getAudioMediaId() {
        return audioMediaId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    void update(Long audioMediaId, Integer durationMinutes) {
        this.audioMediaId = requirePositiveId(audioMediaId);
        this.durationMinutes = requireNonNegativeDuration(durationMinutes);
    }

    private static Content requireContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        return content;
    }

    private static Long requirePositiveId(Long audioMediaId) {
        if (audioMediaId == null || audioMediaId <= 0) {
            throw new IllegalArgumentException("Lullaby audio media ID must be positive");
        }
        return audioMediaId;
    }

    private static Integer requireNonNegativeDuration(Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 0) {
            throw new IllegalArgumentException("Lullaby duration minutes must be non-negative");
        }
        return durationMinutes;
    }
}
