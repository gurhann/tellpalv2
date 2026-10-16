package com.tellpal.v2.asset.domain.reading;

/** An ownership/immutable-equality rejection, kept distinct from persistence API-usage failures. */
public class ReadingAttemptRejectedException extends RuntimeException {
    public ReadingAttemptRejectedException(String reason) { super(reason); }
}
