package com.tellpal.v2.category.api;

import java.util.List;
import java.util.Optional;

import com.tellpal.v2.content.api.PublicContentSummary;
import com.tellpal.v2.shared.domain.LanguageCode;

public interface PublicCategoryQueryApi {

    List<PublicCategoryView> listCategories(LanguageCode languageCode, CategoryApiType type);

    Optional<PublicCategoryView> findCategory(String slug, LanguageCode languageCode);

    List<PublicContentSummary> listCategoryContents(String slug, LanguageCode languageCode, String requestedAccessKey);
}
