package com.tellpal.v2.shared.infrastructure.firebase;

public class FirebaseStorageException extends RuntimeException {

    public FirebaseStorageException(String message) {
        super(message);
    }

    public FirebaseStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
