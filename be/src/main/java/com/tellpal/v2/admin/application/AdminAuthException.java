package com.tellpal.v2.admin.application;

public class AdminAuthException extends RuntimeException {

    public AdminAuthException(String message) {
        super(message);
    }

    public AdminAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
