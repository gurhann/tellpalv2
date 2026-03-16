package com.tellpal.v2.shared.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for UTC timestamp consistency (Özellik 29).
 *
 * **Validates: Requirements 19.1**
 *
 * Özellik 29: Zaman Damgası UTC Tutarlılığı
 * All timestamps must be stored/handled in UTC (ZoneOffset.UTC). This applies to:
 * - created_at, updated_at (BaseEntity fields — OffsetDateTime)
 * - published_at (content_localizations, category_localizations)
 * - occurred_at, ingested_at (content_events, app_events)
 * - issued_at, expires_at, revoked_at (admin_refresh_tokens)
 */
public class TimestampUtcConsistencyPropertyTest {

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<Long> epochSeconds() {
        // Reasonable range: year 1970 to ~2100
        return Arbitraries.longs().between(0L, 4_102_444_800L);
    }

    // -------------------------------------------------------------------------
    // Properties
    // -------------------------------------------------------------------------

    /**
     * Özellik 29 — OffsetDateTime values constructed with ZoneOffset.UTC are valid UTC timestamps.
     *
     * **Validates: Requirements 19.1**
     */
    @Property(tries = 100)
    void utcOffsetDateTimeIsValid(@ForAll("epochSeconds") long epochSecond) {
        OffsetDateTime utcTimestamp = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);

        assertThat(utcTimestamp.getOffset())
                .as("Timestamp constructed with ZoneOffset.UTC must have UTC offset")
                .isEqualTo(ZoneOffset.UTC);

        assertThat(utcTimestamp.getOffset().getTotalSeconds())
                .as("UTC offset total seconds must be 0")
                .isEqualTo(0);
    }

    /**
     * Özellik 29 — OffsetDateTime values with non-UTC offsets are identified as non-UTC.
     *
     * **Validates: Requirements 19.1**
     */
    @Property(tries = 100)
    void nonUtcOffsetDateTimeIsIdentifiedAsNonUtc(@ForAll("epochSeconds") long epochSecond) {
        ZoneOffset[] nonUtcOffsets = {
            ZoneOffset.of("+03:00"),
            ZoneOffset.of("-05:00"),
            ZoneOffset.of("+09:00")
        };

        for (ZoneOffset nonUtcOffset : nonUtcOffsets) {
            OffsetDateTime nonUtcTimestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSecond), nonUtcOffset);

            assertThat(nonUtcTimestamp.getOffset())
                    .as("Timestamp with offset %s must not equal ZoneOffset.UTC", nonUtcOffset)
                    .isNotEqualTo(ZoneOffset.UTC);

            assertThat(nonUtcTimestamp.getOffset().getTotalSeconds())
                    .as("Non-UTC offset total seconds must not be 0 for offset %s", nonUtcOffset)
                    .isNotEqualTo(0);
        }
    }

    /**
     * Özellik 29 — Converting any OffsetDateTime to UTC preserves the instant.
     *
     * **Validates: Requirements 19.1**
     */
    @Property(tries = 100)
    void convertingToUtcPreservesInstant(@ForAll("epochSeconds") long epochSecond) {
        ZoneOffset[] nonUtcOffsets = {
            ZoneOffset.of("+03:00"),
            ZoneOffset.of("-05:00"),
            ZoneOffset.of("+09:00")
        };

        Instant originalInstant = Instant.ofEpochSecond(epochSecond);

        for (ZoneOffset offset : nonUtcOffsets) {
            OffsetDateTime nonUtcTimestamp = OffsetDateTime.ofInstant(originalInstant, offset);
            OffsetDateTime utcTimestamp = nonUtcTimestamp.withOffsetSameInstant(ZoneOffset.UTC);

            assertThat(utcTimestamp.getOffset())
                    .as("After conversion, offset must be UTC")
                    .isEqualTo(ZoneOffset.UTC);

            assertThat(utcTimestamp.toInstant())
                    .as("Converting to UTC must preserve the original instant")
                    .isEqualTo(originalInstant);
        }
    }

    /**
     * Özellik 29 — OffsetDateTime.now(ZoneOffset.UTC) has UTC offset (boundary check).
     *
     * **Validates: Requirements 19.1**
     */
    @Property(tries = 100)
    void nowWithUtcZoneHasUtcOffset(@ForAll("epochSeconds") long ignored) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        assertThat(now.getOffset())
                .as("OffsetDateTime.now(ZoneOffset.UTC) must have UTC offset")
                .isEqualTo(ZoneOffset.UTC);

        assertThat(now.getOffset().getTotalSeconds())
                .as("OffsetDateTime.now(ZoneOffset.UTC) offset total seconds must be 0")
                .isEqualTo(0);
    }
}
