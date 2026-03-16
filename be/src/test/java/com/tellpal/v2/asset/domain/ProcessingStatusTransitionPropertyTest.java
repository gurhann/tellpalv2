package com.tellpal.v2.asset.domain;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based test for ProcessingStatus transition validation (Özellik 42).
 *
 * **Validates: Requirements 22.3, 22.13, 22.14**
 *
 * Özellik 42: İşlem Durumu Geçiş Validasyonu
 * - PENDING → PROCESSING is the only valid start transition.
 * - PROCESSING → COMPLETED or FAILED are the only valid completion transitions.
 * - FAILED → PENDING allows retry.
 * - COMPLETED is a terminal state — no further transitions allowed.
 * - Invalid transitions throw IllegalStateException.
 */
public class ProcessingStatusTransitionPropertyTest {

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<ProcessingStatus> anyStatus() {
        return Arbitraries.of(ProcessingStatus.values());
    }

    // -------------------------------------------------------------------------
    // Properties — valid transitions
    // -------------------------------------------------------------------------

    /**
     * Özellik 42 — PENDING can only transition to PROCESSING.
     * Validates: Requirement 22.3
     */
    @Property(tries = 100)
    void pendingCanOnlyTransitionToProcessing() {
        assertThat(ProcessingStatus.PENDING.canTransitionTo(ProcessingStatus.PROCESSING)).isTrue();
        assertThat(ProcessingStatus.PENDING.canTransitionTo(ProcessingStatus.COMPLETED)).isFalse();
        assertThat(ProcessingStatus.PENDING.canTransitionTo(ProcessingStatus.FAILED)).isFalse();
        assertThat(ProcessingStatus.PENDING.canTransitionTo(ProcessingStatus.PENDING)).isFalse();
    }

    /**
     * Özellik 42 — PROCESSING can transition to COMPLETED or FAILED.
     * Validates: Requirement 22.13
     */
    @Property(tries = 100)
    void processingCanTransitionToCompletedOrFailed() {
        assertThat(ProcessingStatus.PROCESSING.canTransitionTo(ProcessingStatus.COMPLETED)).isTrue();
        assertThat(ProcessingStatus.PROCESSING.canTransitionTo(ProcessingStatus.FAILED)).isTrue();
        assertThat(ProcessingStatus.PROCESSING.canTransitionTo(ProcessingStatus.PENDING)).isFalse();
        assertThat(ProcessingStatus.PROCESSING.canTransitionTo(ProcessingStatus.PROCESSING)).isFalse();
    }

    /**
     * Özellik 42 — FAILED can only transition back to PENDING (retry).
     * Validates: Requirement 22.14
     */
    @Property(tries = 100)
    void failedCanOnlyTransitionToPending() {
        assertThat(ProcessingStatus.FAILED.canTransitionTo(ProcessingStatus.PENDING)).isTrue();
        assertThat(ProcessingStatus.FAILED.canTransitionTo(ProcessingStatus.PROCESSING)).isFalse();
        assertThat(ProcessingStatus.FAILED.canTransitionTo(ProcessingStatus.COMPLETED)).isFalse();
        assertThat(ProcessingStatus.FAILED.canTransitionTo(ProcessingStatus.FAILED)).isFalse();
    }

    /**
     * Özellik 42 — COMPLETED is a terminal state with no valid transitions.
     * Validates: Requirement 22.13
     */
    @Property(tries = 100)
    void completedIsTerminalState() {
        for (ProcessingStatus next : ProcessingStatus.values()) {
            assertThat(ProcessingStatus.COMPLETED.canTransitionTo(next))
                    .as("COMPLETED must not transition to %s", next)
                    .isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Properties — transitionTo() happy path
    // -------------------------------------------------------------------------

    /**
     * Özellik 42 — transitionTo() returns the target status on valid transitions.
     * Validates: Requirement 22.3
     */
    @Property(tries = 100)
    void transitionToReturnsTargetStatusOnValidTransition() {
        assertThat(ProcessingStatus.PENDING.transitionTo(ProcessingStatus.PROCESSING))
                .isEqualTo(ProcessingStatus.PROCESSING);

        assertThat(ProcessingStatus.PROCESSING.transitionTo(ProcessingStatus.COMPLETED))
                .isEqualTo(ProcessingStatus.COMPLETED);

        assertThat(ProcessingStatus.PROCESSING.transitionTo(ProcessingStatus.FAILED))
                .isEqualTo(ProcessingStatus.FAILED);

        assertThat(ProcessingStatus.FAILED.transitionTo(ProcessingStatus.PENDING))
                .isEqualTo(ProcessingStatus.PENDING);
    }

    // -------------------------------------------------------------------------
    // Properties — transitionTo() error path
    // -------------------------------------------------------------------------

    /**
     * Özellik 42 — transitionTo() throws IllegalStateException on invalid transitions.
     * Validates: Requirement 22.3, 22.13, 22.14
     */
    @Property(tries = 100)
    void transitionToThrowsOnInvalidTransition(@ForAll("anyStatus") ProcessingStatus from,
                                                @ForAll("anyStatus") ProcessingStatus to) {
        if (!from.canTransitionTo(to)) {
            assertThatThrownBy(() -> from.transitionTo(to))
                    .as("transitionTo(%s → %s) must throw IllegalStateException", from, to)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Properties — structural invariants
    // -------------------------------------------------------------------------

    /**
     * Özellik 42 — No status can transition to itself (no self-loops).
     * Validates: Requirement 22.3
     */
    @Property(tries = 100)
    void noStatusCanTransitionToItself(@ForAll("anyStatus") ProcessingStatus status) {
        assertThat(status.canTransitionTo(status))
                .as("Status %s must not transition to itself", status)
                .isFalse();
    }

    /**
     * Özellik 42 — Every non-terminal status has at least one valid next state.
     * Validates: Requirement 22.3
     */
    @Property(tries = 100)
    void everyNonTerminalStatusHasAtLeastOneValidTransition() {
        List<ProcessingStatus> nonTerminal = Arrays.asList(
                ProcessingStatus.PENDING,
                ProcessingStatus.PROCESSING,
                ProcessingStatus.FAILED
        );

        for (ProcessingStatus status : nonTerminal) {
            long validTransitions = Arrays.stream(ProcessingStatus.values())
                    .filter(status::canTransitionTo)
                    .count();

            assertThat(validTransitions)
                    .as("Non-terminal status %s must have at least one valid transition", status)
                    .isGreaterThanOrEqualTo(1);
        }
    }

    /**
     * Özellik 42 — The full happy-path lifecycle PENDING→PROCESSING→COMPLETED is valid.
     * Validates: Requirement 22.13
     */
    @Property(tries = 100)
    void happyPathLifecycleIsValid() {
        ProcessingStatus s = ProcessingStatus.PENDING;
        s = s.transitionTo(ProcessingStatus.PROCESSING);
        assertThat(s).isEqualTo(ProcessingStatus.PROCESSING);
        s = s.transitionTo(ProcessingStatus.COMPLETED);
        assertThat(s).isEqualTo(ProcessingStatus.COMPLETED);
    }

    /**
     * Özellik 42 — The retry lifecycle PENDING→PROCESSING→FAILED→PENDING is valid.
     * Validates: Requirement 22.14
     */
    @Property(tries = 100)
    void retryLifecycleIsValid() {
        ProcessingStatus s = ProcessingStatus.PENDING;
        s = s.transitionTo(ProcessingStatus.PROCESSING);
        s = s.transitionTo(ProcessingStatus.FAILED);
        assertThat(s).isEqualTo(ProcessingStatus.FAILED);
        s = s.transitionTo(ProcessingStatus.PENDING);
        assertThat(s).isEqualTo(ProcessingStatus.PENDING);
    }
}
