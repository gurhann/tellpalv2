package com.tellpal.v2.asset.api;

/** A superseded/expired callback is rejected without changing the active job or ready manifest. */
public class StaleListingCoverAttemptException extends RuntimeException {
    public StaleListingCoverAttemptException() {super("Listing cover worker no longer owns this attempt");}
}
