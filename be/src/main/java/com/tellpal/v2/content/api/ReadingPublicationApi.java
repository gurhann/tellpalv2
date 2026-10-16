package com.tellpal.v2.content.api;

/** Internal local/test publication eligibility; identities only, never delivery or session rights. */
public interface ReadingPublicationApi {
    /** Returns current preparation feedback and any separately retained eligible revision. */
    Status status(long contentId, String language);
    /** Explicitly requeues only the current failed intent. */
    Status retry(long contentId, String language);
    record Status(boolean enabled, String contract, String state, Long generation, String liveRevision, String diagnostic, boolean retryable, Long liveGeneration, String liveManifestDigest, long contentId, String language) {
    }
}
