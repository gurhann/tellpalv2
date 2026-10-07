package com.tellpal.v2.category.api;

import java.util.List;
import com.tellpal.v2.content.api.ContentPreview;
import com.tellpal.v2.content.api.StoryDetailMetadata.Credit;
import com.tellpal.v2.asset.api.StoryDetailHeroQueryApi.Hero;

/** Versioned cover-only detail contract; audience is a server-owned test fixture. */
public record LibraryStoryDetail(int schemaVersion, long categoryId, long contentId, String externalKey,
        String type, String experienceType, String requestedLanguage, String contentLanguage,
        LibraryHomeQueryApi.Audience audience, String title, Integer ageRange, Integer durationMinutes,
        String summary, List<Credit> credits, boolean isFree, boolean isLocked, Hero hero,
        List<ContentPreview> recommendations) {
    public LibraryStoryDetail { credits=List.copyOf(credits); recommendations=List.copyOf(recommendations); }
}
