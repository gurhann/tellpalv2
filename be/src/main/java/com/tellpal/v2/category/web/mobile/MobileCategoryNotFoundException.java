package com.tellpal.v2.category.web.mobile;

final class MobileCategoryNotFoundException extends RuntimeException {

    MobileCategoryNotFoundException(String slug, String languageCode) {
        super("Public category %s is not available for language %s".formatted(slug, languageCode));
    }
}
