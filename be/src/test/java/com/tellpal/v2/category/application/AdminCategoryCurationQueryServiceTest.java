package com.tellpal.v2.category.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tellpal.v2.category.api.AdminCategoryContentView;
import com.tellpal.v2.category.domain.Category;
import com.tellpal.v2.category.domain.CategoryRepository;
import com.tellpal.v2.category.domain.CategoryType;
import com.tellpal.v2.category.domain.LocalizationStatus;
import com.tellpal.v2.content.api.EligibleContentQueryApi;
import com.tellpal.v2.content.api.LocalizedContentIdentityLookupApi;
import com.tellpal.v2.content.api.LocalizedContentIdentityReference;
import com.tellpal.v2.shared.domain.LanguageCode;

@ExtendWith(MockitoExtension.class)
class AdminCategoryCurationQueryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EligibleContentQueryApi eligibleContentQueryApi;

    @Mock
    private LocalizedContentIdentityLookupApi localizedContentIdentityLookupApi;

    private AdminCategoryCurationQueryService adminCategoryCurationQueryService;

    @BeforeEach
    void setUp() {
        adminCategoryCurationQueryService = new AdminCategoryCurationQueryService(
                categoryRepository,
                eligibleContentQueryApi,
                localizedContentIdentityLookupApi);
    }

    @Test
    void listCategoryContentsHydratesStoredRowsWithLocalizedIdentity() {
        Category category = Category.create("featured-sleep", CategoryType.STORY, false, true);
        ReflectionTestUtils.setField(category, "id", 42L, Long.class);
        category.upsertLocalization(
                LanguageCode.TR,
                "One Cikan Uyku",
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z"));
        category.addContent(LanguageCode.TR, 77L, 0);

        when(categoryRepository.findById(42L)).thenReturn(Optional.of(category));
        when(localizedContentIdentityLookupApi.findLocalizedIdentities(
                java.util.List.of(77L),
                LanguageCode.TR)).thenReturn(Map.of(
                        77L,
                        new LocalizedContentIdentityReference(
                                77L,
                                "story.ecenin-fasulye-deneyi",
                                "Ecenin Fasulye Deneyi",
                                LanguageCode.TR)));

        assertThat(adminCategoryCurationQueryService.listCategoryContents(42L, LanguageCode.TR))
                .containsExactly(new AdminCategoryContentView(
                        42L,
                        LanguageCode.TR,
                        77L,
                        0,
                        "story.ecenin-fasulye-deneyi",
                        "Ecenin Fasulye Deneyi"));
    }

    @Test
    void listCategoryContentsFallsBackWhenLocalizedIdentityCannotBeResolved() {
        Category category = Category.create("featured-sleep", CategoryType.STORY, false, true);
        ReflectionTestUtils.setField(category, "id", 42L, Long.class);
        category.upsertLocalization(
                LanguageCode.TR,
                "One Cikan Uyku",
                null,
                null,
                LocalizationStatus.PUBLISHED,
                Instant.parse("2026-01-01T00:00:00Z"));
        category.addContent(LanguageCode.TR, 77L, 0);

        when(categoryRepository.findById(42L)).thenReturn(Optional.of(category));
        when(localizedContentIdentityLookupApi.findLocalizedIdentities(
                java.util.List.of(77L),
                LanguageCode.TR)).thenReturn(Map.of());

        AdminCategoryContentView response =
                adminCategoryCurationQueryService.listCategoryContents(42L, LanguageCode.TR).getFirst();

        assertThat(response.externalKey()).isEqualTo("content-77");
        assertThat(response.localizedTitle()).isNull();
    }
}
