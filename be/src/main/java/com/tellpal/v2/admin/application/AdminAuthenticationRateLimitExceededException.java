package com.tellpal.v2.admin.application;

import java.time.Duration;

public class AdminAuthenticationRateLimitExceededException extends RuntimeException {

    private final Duration retryAfter;

    public AdminAuthenticationRateLimitExceededException(String message, Duration retryAfter) {
        super(message);
        if (retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()) {
            this.retryAfter = Duration.ofSeconds(1);
        } else {
            this.retryAfter = retryAfter;
        }
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
