package com.tellpal.v2.user.api;

/**
 * Authentication failure visible to callers of user resolution APIs.
 */
public class UserAuthenticationException extends RuntimeException {

    public UserAuthenticationException(String message) {
        super(message);
    }

    public UserAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
