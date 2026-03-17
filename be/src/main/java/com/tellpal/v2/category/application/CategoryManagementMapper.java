package com.tellpal.v2.category.application;

import com.tellpal.v2.category.application.CategoryManagementResults.CategoryContentRecord;
import com.tellpal.v2.category.application.CategoryManagementResults.CategoryLocalizationRecord;
import com.tellpal.v2.category.domain.CategoryContent;
import com.tellpal.v2.category.domain.CategoryLocalization;

final class CategoryManagementMapper {

    private CategoryManagementMapper() {
    }

    static CategoryLocalizationRecord toLocalizationRecord(Long categoryId, CategoryLocalization localization) {
        return new CategoryLocalizationRecord(
                categoryId,
                localization.getLanguageCode(),
                localization.getName(),
                localization.getDescription(),
                localization.getImageMediaId(),
                localization.getStatus(),
                localization.getPublishedAt(),
                localization.isPublished());
    }

    static CategoryContentRecord toCategoryContentRecord(Long categoryId, CategoryContent categoryContent) {
        return new CategoryContentRecord(
                categoryId,
                categoryContent.getLanguageCode(),
                categoryContent.getContentId(),
                categoryContent.getDisplayOrder());
    }
}
