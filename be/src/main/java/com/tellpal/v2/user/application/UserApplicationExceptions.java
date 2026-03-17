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
}
