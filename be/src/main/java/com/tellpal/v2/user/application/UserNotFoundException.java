package com.tellpal.v2.user.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }

    public UserNotFoundException(String firebaseUid) {
        super("User not found for firebase_uid: " + firebaseUid);
    }
}
