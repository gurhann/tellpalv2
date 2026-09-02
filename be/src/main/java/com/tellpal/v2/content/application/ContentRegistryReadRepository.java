package com.tellpal.v2.content.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.tellpal.v2.content.api.AdminContentRegistryReadiness;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read-side port for the paged CMS content registry.
 *
 * <p>Its implementation evaluates registry predicates and readiness in PostgreSQL before it
 * selects a page. It deliberately returns compact rows rather than content aggregates because
 * readiness depends on every selected-language story-page payload.
 */
public interface ContentRegistryReadRepository {

    RegistryPage findPage(RegistryQuery query);

    List<RegistrySnapshotRow> findSnapshots(Collection<Long> contentIds, LanguageCode languageCode);

    record RegistryQuery(
            LanguageCode languageCode,
            ContentApiType type,
            AdminContentRegistryReadiness readiness,
            String normalizedQuery,
            int page,
            int size) {
    }

    record RegistryPage(List<RegistryCandidate> candidates, long totalItems) {
        public RegistryPage {
            candidates = candidates == null ? List.of() : List.copyOf(candidates);
        }
    }

    record RegistryCandidate(
            Long contentId,
            Instant lastEditedAt,
            AdminContentRegistryReadiness readiness) {
    }

    /** One selected-language page payload; one row is returned per story page when present. */
    record RegistrySnapshotRow(
            Long contentId,
            ContentApiType type,
            String externalKey,
            Integer pageCount,
            boolean active,
            String title,
            String description,
            Long coverMediaId,
            String localizationStatus,
            String processingStatus,
            Integer storyPageNumber,
            Long storyPageLocalizationId,
            String storyPageBodyText,
            Long storyPageAudioMediaId,
            Long storyPageIllustrationMediaId) {
    }
}
