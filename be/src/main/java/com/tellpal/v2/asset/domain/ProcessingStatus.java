package com.tellpal.v2.asset.domain;

public enum ProcessingStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(ProcessingStatus next) {
        return switch (this) {
            case PENDING -> next == PROCESSING;
            case PROCESSING -> next == COMPLETED || next == FAILED;
            case FAILED -> next == PENDING;
            case COMPLETED -> false;
        };
    }

    public ProcessingStatus transitionTo(ProcessingStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException("Cannot transition from " + this + " to " + next);
        }
        return next;
    }
}
