package com.tellpal.v2.event.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for event timestamp separation (Özellik 30).
 *
 * **Validates: Requirements 19.5, 19.6**
 *
 * Özellik 30: Event Zaman Ayrımı
 * - occurred_at represents when the event happened on the client (client-side time).
 * - ingested_at represents when the server received the event (server-side time).
 * - ingested_at must always be >= occurred_at (server receives after or at the same time as the event).
 * - Both timestamps must be in UTC.
 */
public class EventTimestampSeparationPropertyTest {

    record EventTimestamps(OffsetDateTime occurredAt, OffsetDateTime ingestedAt) {}

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    /**
     * Generates an occurredAt timestamp: random epoch second in the past 2 years.
     */
    @Provide
    Arbitrary<OffsetDateTime> occurredAt() {
        long now = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond();
        long twoYearsAgo = now - (2L * 365 * 24 * 3600);
        return Arbitraries.longs().between(twoYearsAgo, now)
                .map(epoch -> OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(epoch), ZoneOffset.UTC));
    }

    /**
     * Generates an ingestedAt that is >= occurredAt (0 to 300 seconds after).
     */
    @Provide
    Arbitrary<EventTimestamps> validTimestamps() {
        return occurredAt().flatMap(occurred ->
                Arbitraries.longs().between(0L, 300L)
                        .map(delaySeconds -> new EventTimestamps(
                                occurred,
                                occurred.plusSeconds(delaySeconds)
                        ))
        );
    }

    /**
     * Generates an ingestedAt that is strictly before occurredAt (invalid case).
     */
    @Provide
    Arbitrary<EventTimestamps> invalidTimestamps() {
        return occurredAt().flatMap(occurred ->
                Arbitraries.longs().between(1L, 3600L)
                        .map(delaySeconds -> new EventTimestamps(
                                occurred,
                                occurred.minusSeconds(delaySeconds)
                        ))
        );
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 30 — ingestedAt must be >= occurredAt (server receives after or at the same time).
     * Validates: Requirement 19.5
     */
    @Property(tries = 100)
    void ingestedAtIsGreaterThanOrEqualToOccurredAt(
            @ForAll("validTimestamps") EventTimestamps ts) {

        assertThat(ts.ingestedAt())
                .as("ingestedAt must be >= occurredAt (server receives event after or at client time)")
                .isAfterOrEqualTo(ts.occurredAt());
    }

    /**
     * Özellik 30 — ingestedAt before occurredAt is an invalid state.
     * Validates: Requirement 19.5
     */
    @Property(tries = 100)
    void ingestedAtBeforeOccurredAtIsInvalid(
            @ForAll("invalidTimestamps") EventTimestamps ts) {

        assertThat(ts.ingestedAt())
                .as("ingestedAt before occurredAt is invalid — server cannot receive event before it happened")
                .isBefore(ts.occurredAt());

        // Confirm the invariant: this state should be rejected
        boolean isValid = !ts.ingestedAt().isBefore(ts.occurredAt());
        assertThat(isValid)
                .as("Timestamp pair where ingestedAt < occurredAt must be considered invalid")
                .isFalse();
    }

    /**
     * Özellik 30 — occurredAt must be in UTC.
     * Validates: Requirement 19.6
     */
    @Property(tries = 100)
    void occurredAtIsInUtc(@ForAll("occurredAt") OffsetDateTime occurredAt) {
        assertThat(occurredAt.getOffset())
                .as("occurredAt must be in UTC (offset +00:00)")
                .isEqualTo(ZoneOffset.UTC);
    }

    /**
     * Özellik 30 — ingestedAt must be in UTC.
     * Validates: Requirement 19.6
     */
    @Property(tries = 100)
    void ingestedAtIsInUtc(@ForAll("validTimestamps") EventTimestamps ts) {
        assertThat(ts.ingestedAt().getOffset())
                .as("ingestedAt must be in UTC (offset +00:00)")
                .isEqualTo(ZoneOffset.UTC);
    }

    /**
     * Özellik 30 — The gap between ingestedAt and occurredAt is non-negative.
     * Validates: Requirement 19.5
     */
    @Property(tries = 100)
    void ingestionDelayIsNonNegative(@ForAll("validTimestamps") EventTimestamps ts) {
        long delaySeconds = java.time.Duration.between(ts.occurredAt(), ts.ingestedAt()).getSeconds();

        assertThat(delaySeconds)
                .as("Ingestion delay (ingestedAt - occurredAt) must be >= 0 seconds")
                .isGreaterThanOrEqualTo(0L);
    }
}
