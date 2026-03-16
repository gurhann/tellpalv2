package com.tellpal.v2.category.application;

public class CategoryLocalizationNotFoundException extends RuntimeException {

    public CategoryLocalizationNotFoundException(Long categoryId, String languageCode) {
        super("Category localization not found: categoryId=" + categoryId + ", lang=" + languageCode);
    }
}
