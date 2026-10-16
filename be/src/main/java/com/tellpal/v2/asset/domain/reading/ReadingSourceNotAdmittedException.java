package com.tellpal.v2.asset.domain.reading;

/** Finite admission rejection, distinct from persistence failures. */
public class ReadingSourceNotAdmittedException extends RuntimeException {
    public ReadingSourceNotAdmittedException() {
        super("SOURCE_NOT_ADMITTED");
    }
}
