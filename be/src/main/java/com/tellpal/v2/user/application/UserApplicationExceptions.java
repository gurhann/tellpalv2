package com.tellpal.v2.user.application;

public final class UserApplicationExceptions {

    private UserApplicationExceptions() {
    }

    public static final class FirebaseTokenVerificationException extends RuntimeException {

        public FirebaseTokenVerificationException(String message) {
            super(message);
        }

        public FirebaseTokenVerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class AppUserNotFoundException extends RuntimeException {

        public AppUserNotFoundException(Long userId) {
            super("App user not found: " + userId);
        }
    }

    public static final class UserProfileNotFoundException extends RuntimeException {

        public UserProfileNotFoundException(Long userId, Long profileId) {
            super("User profile not found for user " + userId + ": " + profileId);
        }
    }

    public static final class AssetReferenceNotFoundException extends RuntimeException {

        public AssetReferenceNotFoundException(String fieldName, Long assetId) {
            super("Asset not found for " + fieldName + ": " + assetId);
        }
    }

    public static final class AssetMediaTypeMismatchException extends RuntimeException {

        public AssetMediaTypeMismatchException(
                String fieldName,
                Long assetId,
                com.tellpal.v2.asset.api.AssetMediaType expectedMediaType,
                com.tellpal.v2.asset.api.AssetMediaType actualMediaType) {
            super("Asset " + assetId + " for " + fieldName + " must be "
                    + expectedMediaType + " but was " + actualMediaType);
        }
    }
}
