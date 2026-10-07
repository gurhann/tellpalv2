package com.tellpal.v2.content.api;

import java.util.List;
import com.tellpal.v2.asset.api.StoryDetailHeroQueryApi.Hero;

/** Scalar, access-aware reading metadata; contributor identities are not entities. */
public record StoryDetailMetadata(long contentId, String externalKey, String title,
        Integer ageRange, Integer durationMinutes, String summary, List<Credit> credits,
        boolean isFree, boolean isLocked, Hero hero) {
    public StoryDetailMetadata { credits = List.copyOf(credits); }
    public record Credit(long contributorId, String role, String displayName) {}
}
