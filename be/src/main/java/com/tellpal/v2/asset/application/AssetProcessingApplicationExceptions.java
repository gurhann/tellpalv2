package com.tellpal.v2.asset.application;

import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.asset.api.AssetProcessingTarget;

public final class AssetProcessingApplicationExceptions {

    private AssetProcessingApplicationExceptions() {
    }

    public static final class AssetProcessingNotFoundException extends RuntimeException {

        public AssetProcessingNotFoundException(AssetProcessingTarget target) {
            super("Asset processing not found for " + describe(target));
        }

        public AssetProcessingNotFoundException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingAlreadyRunningException extends RuntimeException {

        public AssetProcessingAlreadyRunningException(AssetProcessingTarget target) {
            super("Asset processing is already running for " + describe(target));
        }

        public AssetProcessingAlreadyRunningException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingAlreadyPendingException extends RuntimeException {

        public AssetProcessingAlreadyPendingException(AssetProcessingTarget target) {
            super("Asset processing is already pending for " + describe(target));
        }

        public AssetProcessingAlreadyPendingException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingAlreadyCompletedException extends RuntimeException {

        public AssetProcessingAlreadyCompletedException(AssetProcessingTarget target) {
            super("Asset processing is already completed for " + describe(target));
        }

        public AssetProcessingAlreadyCompletedException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingRetryRequiredException extends RuntimeException {

        public AssetProcessingRetryRequiredException(AssetProcessingTarget target) {
            super("Asset processing must be retried before it can be scheduled again for " + describe(target));
        }

        public AssetProcessingRetryRequiredException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingLocalizationNotFoundException extends RuntimeException {

        public AssetProcessingLocalizationNotFoundException(AssetProcessingTarget target) {
            super("Content localization not found for asset processing: " + describe(target));
        }

        public AssetProcessingLocalizationNotFoundException(Long contentId, LanguageCode languageCode) {
            this(AssetProcessingTarget.localization(contentId, languageCode));
        }
    }

    public static final class AssetProcessingContentNotFoundException extends RuntimeException {

        public AssetProcessingContentNotFoundException(AssetProcessingTarget target) {
            super("Content not found for asset processing: " + describe(target));
        }
    }

    private static String describe(AssetProcessingTarget target) {
        if (target.isContent()) {
            return "content " + target.contentId();
        }
        return "content " + target.contentId() + " and language " + target.languageCode().value();
    }
}
