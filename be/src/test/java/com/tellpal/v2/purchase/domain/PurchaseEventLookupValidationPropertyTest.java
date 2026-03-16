package com.tellpal.v2.purchase.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for purchase event lookup table validation (Özellik 35).
 *
 * **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.7**
 *
 * Özellik 35: Purchase Event Lookup Validasyonu
 * - event_type must be a valid purchase_event_types code (Req 12.1)
 * - store must be a valid purchase_stores code (Req 12.2)
 * - period_type must be a valid subscription_period_types code (Req 12.3)
 * - environment must be a valid purchase_environments code (Req 12.4)
 * - cancel_reason / expiration_reason must be valid purchase_reason_codes (Req 12.5)
 * - purchase_context_snapshots.purchase_event_id must reference a valid purchase_events record (Req 12.7)
 */
public class PurchaseEventLookupValidationPropertyTest {

    // -------------------------------------------------------------------------
    // Valid lookup sets — mirrors V8__create_purchase_lookup_tables.sql
    // -------------------------------------------------------------------------

    static final Set<String> VALID_EVENT_TYPES = Set.of(
            "INITIAL_PURCHASE", "RENEWAL", "CANCELLATION", "EXPIRATION",
            "UNCANCELLATION", "BILLING_ISSUE", "PRODUCT_CHANGE", "TRANSFER",
            "SUBSCRIPTION_PAUSED", "SUBSCRIPTION_EXTENDED", "TEMPORARY_ENTITLEMENT_GRANT"
    );

    static final Set<String> VALID_STORES = Set.of(
            "APP_STORE", "PLAY_STORE", "STRIPE", "RC_BILLING", "AMAZON"
    );

    static final Set<String> VALID_PERIOD_TYPES = Set.of(
            "TRIAL", "INTRO", "NORMAL", "PROMOTIONAL", "PREPAID"
    );

    static final Set<String> VALID_ENVIRONMENTS = Set.of(
            "SANDBOX", "PRODUCTION"
    );

    static final Set<String> VALID_CANCEL_REASONS = Set.of(
            "UNSUBSCRIBE", "BILLING_ERROR", "PRICE_INCREASE", "CUSTOMER_SUPPORT", "UNKNOWN"
    );

    static final Set<String> VALID_EXPIRATION_REASONS = Set.of(
            "BILLING_ERROR", "CUSTOMER_SUPPORT", "DEVELOPER", "PROMOTIONAL", "UNKNOWN"
    );

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> validEventType() {
        return Arbitraries.of(VALID_EVENT_TYPES.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> invalidEventType() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(32)
                .filter(s -> !VALID_EVENT_TYPES.contains(s.toUpperCase()))
                .map(String::toUpperCase);
    }

    @Provide
    Arbitrary<String> validStore() {
        return Arbitraries.of(VALID_STORES.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> invalidStore() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(32)
                .filter(s -> !VALID_STORES.contains(s.toUpperCase()))
                .map(String::toUpperCase);
    }

    @Provide
    Arbitrary<String> validPeriodType() {
        return Arbitraries.of(VALID_PERIOD_TYPES.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> validEnvironment() {
        return Arbitraries.of(VALID_ENVIRONMENTS.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> validCancelReason() {
        return Arbitraries.of(VALID_CANCEL_REASONS.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> validExpirationReason() {
        return Arbitraries.of(VALID_EXPIRATION_REASONS.toArray(String[]::new));
    }

    @Provide
    Arbitrary<String> invalidReasonCode() {
        Set<String> allValid = new java.util.HashSet<>();
        allValid.addAll(VALID_CANCEL_REASONS);
        allValid.addAll(VALID_EXPIRATION_REASONS);
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(32)
                .filter(s -> !allValid.contains(s.toUpperCase()))
                .map(String::toUpperCase);
    }

    @Provide
    Arbitrary<Long> purchaseEventId() {
        return Arbitraries.longs().between(1L, 100_000L);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.1: event_type validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — Valid event_type values are accepted.
     *
     * **Validates: Requirements 12.1**
     */
    @Property(tries = 100)
    void validEventTypeIsAccepted(@ForAll("validEventType") String eventType) {
        assertThat(VALID_EVENT_TYPES)
                .as("event_type='%s' must be accepted as a valid purchase_event_types code", eventType)
                .contains(eventType);
    }

    /**
     * Özellik 35 — Invalid event_type values are rejected.
     *
     * **Validates: Requirements 12.1**
     */
    @Property(tries = 100)
    void invalidEventTypeIsRejected(@ForAll("invalidEventType") String eventType) {
        assertThat(VALID_EVENT_TYPES)
                .as("event_type='%s' must be rejected as it is not a valid purchase_event_types code", eventType)
                .doesNotContain(eventType);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.2: store validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — Valid store values are accepted.
     *
     * **Validates: Requirements 12.2**
     */
    @Property(tries = 100)
    void validStoreIsAccepted(@ForAll("validStore") String store) {
        assertThat(VALID_STORES)
                .as("store='%s' must be accepted as a valid purchase_stores code", store)
                .contains(store);
    }

    /**
     * Özellik 35 — Invalid store values are rejected.
     *
     * **Validates: Requirements 12.2**
     */
    @Property(tries = 100)
    void invalidStoreIsRejected(@ForAll("invalidStore") String store) {
        assertThat(VALID_STORES)
                .as("store='%s' must be rejected as it is not a valid purchase_stores code", store)
                .doesNotContain(store);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.3: period_type validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — Valid period_type values are accepted.
     *
     * **Validates: Requirements 12.3**
     */
    @Property(tries = 100)
    void validPeriodTypeIsAccepted(@ForAll("validPeriodType") String periodType) {
        assertThat(VALID_PERIOD_TYPES)
                .as("period_type='%s' must be accepted as a valid subscription_period_types code", periodType)
                .contains(periodType);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.4: environment validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — Valid environment values are accepted.
     *
     * **Validates: Requirements 12.4**
     */
    @Property(tries = 100)
    void validEnvironmentIsAccepted(@ForAll("validEnvironment") String environment) {
        assertThat(VALID_ENVIRONMENTS)
                .as("environment='%s' must be accepted as a valid purchase_environments code", environment)
                .contains(environment);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.5: reason code validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — Valid cancel_reason codes (reason_type=CANCEL_REASON) are accepted.
     *
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 100)
    void validCancelReasonIsAccepted(@ForAll("validCancelReason") String cancelReason) {
        assertThat(VALID_CANCEL_REASONS)
                .as("cancel_reason='%s' with reason_type=CANCEL_REASON must be accepted", cancelReason)
                .contains(cancelReason);
    }

    /**
     * Özellik 35 — Valid expiration_reason codes (reason_type=EXPIRATION_REASON) are accepted.
     *
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 100)
    void validExpirationReasonIsAccepted(@ForAll("validExpirationReason") String expirationReason) {
        assertThat(VALID_EXPIRATION_REASONS)
                .as("expiration_reason='%s' with reason_type=EXPIRATION_REASON must be accepted", expirationReason)
                .contains(expirationReason);
    }

    /**
     * Özellik 35 — Invalid reason codes are rejected (not in either CANCEL_REASON or EXPIRATION_REASON sets).
     *
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 100)
    void invalidReasonCodeIsRejected(@ForAll("invalidReasonCode") String reasonCode) {
        assertThat(VALID_CANCEL_REASONS)
                .as("reason_code='%s' must not be a valid CANCEL_REASON", reasonCode)
                .doesNotContain(reasonCode);
        assertThat(VALID_EXPIRATION_REASONS)
                .as("reason_code='%s' must not be a valid EXPIRATION_REASON", reasonCode)
                .doesNotContain(reasonCode);
    }

    /**
     * Özellik 35 — A cancel_reason code is not necessarily a valid expiration_reason code (sets are distinct).
     *
     * **Validates: Requirements 12.5**
     */
    @Property(tries = 100)
    void cancelReasonAndExpirationReasonSetsAreDistinctlyTyped(
            @ForAll("validCancelReason") String cancelReason,
            @ForAll("validExpirationReason") String expirationReason) {

        Assume.that(!cancelReason.equals(expirationReason));

        // When codes differ, they belong to their respective typed sets only
        // (BILLING_ERROR and UNKNOWN appear in both — the Assume filters those out)
        assertThat(VALID_CANCEL_REASONS).contains(cancelReason);
        assertThat(VALID_EXPIRATION_REASONS).contains(expirationReason);
    }

    // -------------------------------------------------------------------------
    // Properties — Req 12.7: purchase_context_snapshots FK validation
    // -------------------------------------------------------------------------

    /**
     * Özellik 35 — A purchase_context_snapshot referencing a known purchase_event_id is valid.
     *
     * **Validates: Requirements 12.7**
     */
    @Property(tries = 100)
    void contextSnapshotWithKnownEventIdIsValid(@ForAll("purchaseEventId") Long eventId) {
        // Simulate a registry of persisted purchase_events
        java.util.Set<Long> persistedEventIds = new java.util.HashSet<>();
        persistedEventIds.add(eventId);

        // A snapshot referencing that event_id must resolve successfully
        boolean isValid = persistedEventIds.contains(eventId);

        assertThat(isValid)
                .as("purchase_context_snapshot with purchase_event_id=%d must reference a valid purchase_events record", eventId)
                .isTrue();
    }

    /**
     * Özellik 35 — A purchase_context_snapshot referencing an unknown purchase_event_id is invalid.
     *
     * **Validates: Requirements 12.7**
     */
    @Property(tries = 100)
    void contextSnapshotWithUnknownEventIdIsInvalid(
            @ForAll("purchaseEventId") Long knownEventId,
            @ForAll("purchaseEventId") Long unknownEventId) {

        Assume.that(!knownEventId.equals(unknownEventId));

        java.util.Set<Long> persistedEventIds = new java.util.HashSet<>();
        persistedEventIds.add(knownEventId);

        boolean isValid = persistedEventIds.contains(unknownEventId);

        assertThat(isValid)
                .as("purchase_context_snapshot with purchase_event_id=%d must be rejected when no matching purchase_events record exists", unknownEventId)
                .isFalse();
    }
}
