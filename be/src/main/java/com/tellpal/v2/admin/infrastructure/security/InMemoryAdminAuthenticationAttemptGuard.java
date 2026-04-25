package com.tellpal.v2.admin.infrastructure.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.tellpal.v2.admin.application.AdminAuthenticationAttemptGuard;
import com.tellpal.v2.admin.application.AdminAuthenticationRateLimitExceededException;

@Component
class InMemoryAdminAuthenticationAttemptGuard implements AdminAuthenticationAttemptGuard {

    private static final String UNKNOWN_IP_ADDRESS = "unknown";

    private final Clock clock;
    private final AdminSecurityProperties.BruteForceProperties properties;
    private final ConcurrentHashMap<String, AttemptBucket> loginUsernameBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptBucket> loginIpBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptBucket> refreshTokenBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptBucket> refreshIpBuckets = new ConcurrentHashMap<>();

    InMemoryAdminAuthenticationAttemptGuard(Clock clock, AdminSecurityProperties properties) {
        this.clock = clock;
        this.properties = properties.bruteForce();
    }

    @Override
    public void assertLoginAllowed(String username, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        assertAllowed(
                loginUsernameBuckets,
                usernameKey(username),
                properties.maxLoginFailuresPerUsername(),
                now);
        assertAllowed(
                loginIpBuckets,
                ipKey(ipAddress),
                properties.maxLoginFailuresPerIp(),
                now);
    }

    @Override
    public void recordLoginFailure(String username, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        recordFailure(
                loginUsernameBuckets,
                usernameKey(username),
                properties.maxLoginFailuresPerUsername(),
                now);
        recordFailure(
                loginIpBuckets,
                ipKey(ipAddress),
                properties.maxLoginFailuresPerIp(),
                now);
    }

    @Override
    public void clearLoginFailures(String username) {
        if (!properties.enabled()) {
            return;
        }
        loginUsernameBuckets.remove(usernameKey(username));
    }

    @Override
    public void assertRefreshAllowed(String refreshTokenHash, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        assertAllowed(
                refreshTokenBuckets,
                tokenHashKey(refreshTokenHash),
                properties.maxRefreshFailuresPerIp(),
                now);
        assertAllowed(
                refreshIpBuckets,
                ipKey(ipAddress),
                properties.maxRefreshFailuresPerIp(),
                now);
    }

    @Override
    public void recordRefreshFailure(String refreshTokenHash, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        Instant now = Instant.now(clock);
        recordFailure(
                refreshTokenBuckets,
                tokenHashKey(refreshTokenHash),
                properties.maxRefreshFailuresPerIp(),
                now);
        recordFailure(
                refreshIpBuckets,
                ipKey(ipAddress),
                properties.maxRefreshFailuresPerIp(),
                now);
    }

    @Override
    public void clearRefreshFailures(String refreshTokenHash) {
        if (!properties.enabled()) {
            return;
        }
        refreshTokenBuckets.remove(tokenHashKey(refreshTokenHash));
    }

    private void assertAllowed(
            ConcurrentHashMap<String, AttemptBucket> buckets,
            String key,
            int maxFailures,
            Instant now) {
        pruneIfNeeded(buckets, now);
        AttemptBucket bucket = buckets.get(key);
        if (bucket == null) {
            return;
        }
        bucket.assertAllowed(maxFailures, properties.window(), properties.lockoutDuration(), now);
    }

    private void recordFailure(
            ConcurrentHashMap<String, AttemptBucket> buckets,
            String key,
            int maxFailures,
            Instant now) {
        pruneIfNeeded(buckets, now);
        buckets.computeIfAbsent(key, ignored -> new AttemptBucket())
                .recordFailure(maxFailures, properties.window(), properties.lockoutDuration(), now);
    }

    private void pruneIfNeeded(ConcurrentHashMap<String, AttemptBucket> buckets, Instant now) {
        if (buckets.size() < properties.maxEntries()) {
            return;
        }
        for (Map.Entry<String, AttemptBucket> entry : buckets.entrySet()) {
            if (entry.getValue().isIdle(properties.window(), now)) {
                buckets.remove(entry.getKey(), entry.getValue());
            }
        }
        if (buckets.size() < properties.maxEntries()) {
            return;
        }
        int removeCount = Math.max(1, buckets.size() - properties.maxEntries() + 1);
        for (String key : buckets.keySet()) {
            buckets.remove(key);
            removeCount--;
            if (removeCount == 0) {
                return;
            }
        }
    }

    private static String usernameKey(String username) {
        return requireText(username, "Admin username must not be blank").toLowerCase(Locale.ROOT);
    }

    private static String tokenHashKey(String refreshTokenHash) {
        return requireText(refreshTokenHash, "Admin refresh token hash must not be blank");
    }

    private static String ipKey(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return UNKNOWN_IP_ADDRESS;
        }
        return ipAddress.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static final class AttemptBucket {

        private final ArrayDeque<Instant> failures = new ArrayDeque<>();
        private Instant lockedUntil;

        synchronized void assertAllowed(
                int maxFailures,
                Duration window,
                Duration lockoutDuration,
                Instant now) {
            prune(window, now);
            if (lockedUntil != null && lockedUntil.isAfter(now)) {
                throw rateLimited(lockedUntil, now);
            }
            if (failures.size() >= maxFailures) {
                lockedUntil = now.plus(lockoutDuration);
                throw rateLimited(lockedUntil, now);
            }
        }

        synchronized void recordFailure(
                int maxFailures,
                Duration window,
                Duration lockoutDuration,
                Instant now) {
            prune(window, now);
            failures.addLast(now);
            if (failures.size() >= maxFailures) {
                lockedUntil = now.plus(lockoutDuration);
            }
        }

        synchronized boolean isIdle(Duration window, Instant now) {
            prune(window, now);
            return failures.isEmpty() && (lockedUntil == null || !lockedUntil.isAfter(now));
        }

        private void prune(Duration window, Instant now) {
            Instant earliestAllowed = now.minus(window);
            while (!failures.isEmpty() && failures.peekFirst().isBefore(earliestAllowed)) {
                failures.removeFirst();
            }
            if (lockedUntil != null && !lockedUntil.isAfter(now)) {
                lockedUntil = null;
            }
        }

        private static AdminAuthenticationRateLimitExceededException rateLimited(Instant lockedUntil, Instant now) {
            return new AdminAuthenticationRateLimitExceededException(
                    "Too many authentication attempts",
                    Duration.between(now, lockedUntil));
        }
    }
}
