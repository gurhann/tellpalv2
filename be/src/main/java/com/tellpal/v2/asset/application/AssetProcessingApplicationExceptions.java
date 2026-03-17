package com.tellpal.v2.asset.application;

import com.tellpal.v2.shared.domain.LanguageCode;

public final class AssetProcessingApplicationExceptions {

    private AssetProcessingApplicationExceptions() {
    }

    public static final class AssetProcessingNotFoundException extends RuntimeException {

        public AssetProcessingNotFoundException(Long contentId, LanguageCode languageCode) {
            super("Asset processing not found for content " + contentId + " and language " + languageCode.value());
        }
    }

    public static final class AssetProcessingAlreadyRunningException extends RuntimeException {

        public AssetProcessingAlreadyRunningException(Long contentId, LanguageCode languageCode) {
            super("Asset processing is already running for content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }

    public static final class AssetProcessingAlreadyPendingException extends RuntimeException {

        public AssetProcessingAlreadyPendingException(Long contentId, LanguageCode languageCode) {
            super("Asset processing is already pending for content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }

    public static final class AssetProcessingAlreadyCompletedException extends RuntimeException {

        public AssetProcessingAlreadyCompletedException(Long contentId, LanguageCode languageCode) {
            super("Asset processing is already completed for content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }

    public static final class AssetProcessingRetryRequiredException extends RuntimeException {

        public AssetProcessingRetryRequiredException(Long contentId, LanguageCode languageCode) {
            super("Asset processing must be retried before it can be scheduled again for content "
                    + contentId
                    + " and language "
                    + languageCode.value());
        }
    }

    public static final class AssetProcessingLocalizationNotFoundException extends RuntimeException {

        public AssetProcessingLocalizationNotFoundException(Long contentId, LanguageCode languageCode) {
            super("Content localization not found for asset processing: content "
                    + contentId
                    + ", language "
                    + languageCode.value());
        }
    }
}
