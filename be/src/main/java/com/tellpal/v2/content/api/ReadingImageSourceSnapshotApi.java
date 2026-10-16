package com.tellpal.v2.content.api;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Snapshot;

/** Captures complete exact-language STORY/READING bindings without starting publication or media work. */
public interface ReadingImageSourceSnapshotApi {
    /** Returns a consistent exact-language binding snapshot, with explicit reasons when it is incomplete. */
    Snapshot capture(long contentId, String language);
    /** Recompares editable content bindings only; callers must separately validate byte identity and publication eligibility. */
    boolean matches(Snapshot captured);
}
