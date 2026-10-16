package com.tellpal.v2.content.domain.publication;

import java.util.UUID;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Snapshot;

/** Claimed desired generation; the complete token fences attachment, failure and adoption. */
public record ReadingPublication(long contentId, String language, long generation, UUID lease, long leaseExpiresEpochMillis, Snapshot snapshot, UUID jobId, boolean explicitRetry, String effectiveProfileIdentity) {
}
