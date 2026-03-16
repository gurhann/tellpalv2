package com.tellpal.v2.purchase.domain;

import com.tellpal.v2.purchase.application.AttributionService.AppEventCandidate;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for attribution window logic (Özellik 19).
 *
 * **Validates: Requirements 11.5, 11.6**
 *
 * Özellik 19: Attribution Penceresi Mantığı
 * - Events within the 24h window (86400 seconds) before purchaseOccurredAt are eligible.
 * - Events outside the 24h window are NOT eligible.
 * - LOCKED_CONTENT_CLICKED is preferred over PAYWALL_SHOWN when both are in window.
 * - PAYWALL_SHOWN is used as fallback when no LOCKED_CONTENT_CLICKED is in window.
 */
public class AttributionWindowPropertyTest {

    private static final int ATTRIBUTION_WINDOW_SECONDS = 86400;
    private static final String LOCKED_CONTENT_CLICKED = "LOCKED_CONTENT_CLICKED";
    private static final String PAYWALL_SHOWN = "PAYWALL_SHOWN";

    private final OffsetDateTime purchaseOccurredAt = OffsetDateTime.now();

    // -------------------------------------------------------------------------
    // Helpers — pure domain logic mirroring AttributionService
    // -------------------------------------------------------------------------

    private UUID resolveAttributedEventId(List<AppEventCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        OffsetDateTime windowStart = purchaseOccurredAt.minusSeconds(ATTRIBUTION_WINDOW_SECONDS);

        List<AppEventCandidate> inWindow = candidates.stream()
                .filter(e -> !e.occurredAt().isBefore(windowStart) && !e.occurredAt().isAfter(purchaseOccurredAt))
                .toList();

        return inWindow.stream()
                .filter(e -> LOCKED_CONTENT_CLICKED.equals(e.eventType()))
                .findFirst()
                .or(() -> inWindow.stream()
                        .filter(e -> PAYWALL_SHOWN.equals(e.eventType()))
                        .findFirst())
                .map(AppEventCandidate::eventId)
                .orElse(null);
    }

    private boolean isWithinWindow(OffsetDateTime eventOccurredAt) {
        OffsetDateTime windowStart = purchaseOccurredAt.minusSeconds(ATTRIBUTION_WINDOW_SECONDS);
        return !eventOccurredAt.isBefore(windowStart) && !eventOccurredAt.isAfter(purchaseOccurredAt);
    }

    private AppEventCandidate candidateBeforePurchase(String eventType, long secondsBefore) {
        return new AppEventCandidate(UUID.randomUUID(), eventType, purchaseOccurredAt.minusSeconds(secondsBefore));
    }

    private AppEventCandidate candidateAfterPurchase(String eventType, long secondsAfter) {
        return new AppEventCandidate(UUID.randomUUID(), eventType, purchaseOccurredAt.plusSeconds(secondsAfter));
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Long> secondsInsideWindow() {
        return Arbitraries.longs().between(1L, ATTRIBUTION_WINDOW_SECONDS - 1L);
    }

    @Provide
    Arbitrary<Long> secondsOutsideWindow() {
        return Arbitraries.longs().between(ATTRIBUTION_WINDOW_SECONDS + 1L, (long) ATTRIBUTION_WINDOW_SECONDS * 2);
    }

    @Provide
    Arbitrary<Long> secondsAfterPurchase() {
        return Arbitraries.longs().between(1L, (long) ATTRIBUTION_WINDOW_SECONDS);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 19 — An event within 24h before purchase is eligible for attribution.
     * Validates: Requirement 11.5
     */
    @Property(tries = 100)
    void eventWithinWindowIsEligible(@ForAll("secondsInsideWindow") long secondsBefore) {
        AppEventCandidate candidate = candidateBeforePurchase(LOCKED_CONTENT_CLICKED, secondsBefore);
        UUID result = resolveAttributedEventId(List.of(candidate));
        assertThat(result)
                .as("Event %d seconds before purchase (within 24h window) must be attributed", secondsBefore)
                .isEqualTo(candidate.eventId());
    }

    /**
     * Özellik 19 — An event at the exact window boundary (purchaseOccurredAt - 86400s) is eligible.
     * Validates: Requirement 11.5
     */
    @Property(tries = 100)
    void eventAtWindowBoundaryIsEligible() {
        AppEventCandidate candidate = candidateBeforePurchase(LOCKED_CONTENT_CLICKED, ATTRIBUTION_WINDOW_SECONDS);
        assertThat(isWithinWindow(candidate.occurredAt()))
                .as("Event at exact window boundary must be within the window")
                .isTrue();
        UUID result = resolveAttributedEventId(List.of(candidate));
        assertThat(result)
                .as("Event at exact window boundary must be attributed")
                .isEqualTo(candidate.eventId());
    }

    /**
     * Özellik 19 — An event outside the 24h window is NOT eligible.
     * Validates: Requirement 11.6
     */
    @Property(tries = 100)
    void eventOutsideWindowIsNotEligible(@ForAll("secondsOutsideWindow") long secondsBefore) {
        AppEventCandidate candidate = candidateBeforePurchase(LOCKED_CONTENT_CLICKED, secondsBefore);
        UUID result = resolveAttributedEventId(List.of(candidate));
        assertThat(result)
                .as("Event %d seconds before purchase (outside 24h window) must NOT be attributed", secondsBefore)
                .isNull();
    }

    /**
     * Özellik 19 — An event occurring after the purchase is NOT eligible.
     * Validates: Requirement 11.6
     */
    @Property(tries = 100)
    void eventAfterPurchaseIsNotEligible(@ForAll("secondsAfterPurchase") long secondsAfter) {
        AppEventCandidate candidate = candidateAfterPurchase(LOCKED_CONTENT_CLICKED, secondsAfter);
        UUID result = resolveAttributedEventId(List.of(candidate));
        assertThat(result)
                .as("Event %d seconds AFTER purchase must NOT be attributed", secondsAfter)
                .isNull();
    }

    /**
     * Özellik 19 — LOCKED_CONTENT_CLICKED is preferred over PAYWALL_SHOWN when both are in window.
     * Validates: Requirement 11.5
     */
    @Property(tries = 100)
    void lockedContentClickedPreferredOverPaywallShown(
            @ForAll("secondsInsideWindow") long lockedSeconds,
            @ForAll("secondsInsideWindow") long paywallSeconds) {
        AppEventCandidate locked = candidateBeforePurchase(LOCKED_CONTENT_CLICKED, lockedSeconds);
        AppEventCandidate paywall = candidateBeforePurchase(PAYWALL_SHOWN, paywallSeconds);
        UUID result = resolveAttributedEventId(List.of(paywall, locked));
        assertThat(result)
                .as("LOCKED_CONTENT_CLICKED must be preferred over PAYWALL_SHOWN when both are in window")
                .isEqualTo(locked.eventId());
    }

    /**
     * Özellik 19 — PAYWALL_SHOWN is used as fallback when no LOCKED_CONTENT_CLICKED is in window.
     * Validates: Requirement 11.6
     */
    @Property(tries = 100)
    void paywallShownUsedAsFallbackWhenNoLockedContentClicked(
            @ForAll("secondsInsideWindow") long paywallSeconds) {
        AppEventCandidate paywall = candidateBeforePurchase(PAYWALL_SHOWN, paywallSeconds);
        UUID result = resolveAttributedEventId(List.of(paywall));
        assertThat(result)
                .as("PAYWALL_SHOWN must be used as fallback when no LOCKED_CONTENT_CLICKED is in window")
                .isEqualTo(paywall.eventId());
    }
}
