package com.tellpal.v2.category.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

import com.tellpal.v2.shared.domain.LanguageCode;

class CategoryDisplayOrderPropertyTest {

    @Property(tries = 120)
    void duplicateDisplayOrderIsRejectedWithinSameLanguage(@ForAll @IntRange(min = 0, max = 50) int displayOrder) {
        Category category = publishedCategory();

        category.addContent(LanguageCode.TR, 1L, displayOrder);

        assertThatThrownBy(() -> category.addContent(LanguageCode.TR, 2L, displayOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("display order");
    }

    @Property(tries = 120)
    void sameDisplayOrderCanBeReusedAcrossLanguages(@ForAll @IntRange(min = 0, max = 50) int displayOrder) {
        Category category = publishedCategory();

        category.addContent(LanguageCode.TR, 1L, displayOrder);
        category.addContent(LanguageCode.EN, 2L, displayOrder);

        assertThat(category.getCuratedContents()).hasSize(2);
    }

    private static Category publishedCategory() {
        Category category = Category.create("sleep-routines", CategoryType.STORY, false, true);
        category.upsertLocalization(
                LanguageCode.TR,
                "Uyku Rutinleri",
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z"));
        category.upsertLocalization(
                LanguageCode.EN,
                "Sleep Routines",
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z"));
        return category;
    }
}
