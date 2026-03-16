package com.tellpal.v2.content.application;

public class ContentLocalizationNotFoundException extends RuntimeException {

    public ContentLocalizationNotFoundException(Long contentId, String languageCode) {
        super("ContentLocalization not found for contentId=" + contentId + ", lang=" + languageCode);
    }
}
