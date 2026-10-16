package com.tellpal.v2.asset.api;

import java.util.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;

/** Asset-owned P3 proof; no URLs, access grants or ordinary delivery assertion. */
public interface ReadingPublicationReadyApi {
    /** Captures the complete role/limits/encoder/runtime implementation identity outside DB transactions. */
    String effectiveProfileIdentity(Map<Role, TestProfile> profiles);
    /** Captures only explicitly admitted source IDs and schedules P2 work without a transaction. */
    Preparation prepareAdmitted(Snapshot snapshot, Map<Role, TestProfile> profiles);
    /** Explicitly retries the currently failed P2 job. */
    Preparation retry(UUID jobId);
    /** Reads durable state only; COMPLETED is not a fresh adoption proof. */
    Preparation state(UUID jobId);
    /** Fresh pinned verification outside every database transaction. */
    Proof verify(UUID jobId, Map<Role, TestProfile> profiles);
    /** Joins the caller's transaction: locks source admissions in ID order then availability, with no I/O. */
    boolean guard(Proof proof, Snapshot exactCurrent);
    /** Checks retained media's own identity/availability rather than newer content bindings. DB-only. */
    boolean retainedEligible(Proof proof);
    record SourceEpoch(long assetId, UUID admissionId, long epoch) {
    }
    record Proof(UUID jobId, String revision, String manifestDigest, long availabilityEpoch, Snapshot snapshot, List<SourceEpoch> sources, Map<Role, TestProfile> profiles) {
        public Proof {
            sources = List.copyOf(sources);
            profiles = Map.copyOf(profiles);
        }
    }
}
