package com.tellpal.v2.content.application.query;

import java.util.List;
import java.util.Optional;
import com.tellpal.v2.content.api.StoryDetailMetadata.Credit;

/** Bounded scalar projection port, excluding pages, packages and narration. */
public interface StoryDetailMetadataReadRepository {
    record Row(long contentId, String externalKey, String title, Integer ageRange,
            Integer durationMinutes, String summary, Long sourceAssetId) {}
    Optional<Row> findEligible(long contentId, String language);
    List<Credit> credits(long contentId, String language);
}
