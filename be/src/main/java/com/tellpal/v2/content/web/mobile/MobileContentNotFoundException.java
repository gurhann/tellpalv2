package com.tellpal.v2.content.web.mobile;

final class MobileContentNotFoundException extends RuntimeException {

    MobileContentNotFoundException(Long contentId, String languageCode) {
        super("Public content %s is not available for language %s".formatted(contentId, languageCode));
    }
}
