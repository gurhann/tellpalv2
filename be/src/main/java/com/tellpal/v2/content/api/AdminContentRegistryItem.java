package com.tellpal.v2.content.api;

import java.time.Instant;
import java.util.List;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Compact language-specific row rendered in the CMS content registry.
 */
public record AdminContentRegistryItem(
        Long contentId,
        ContentApiType type,
        String externalKey,
        Integer pageCount,
        LanguageCode selectedLanguage,
        String title,
        AdminContentRegistryReadiness readiness,
        List<AdminContentRegistryBlocker> blockers,
        Instant lastEditedAt) {

    public AdminContentRegistryItem {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
