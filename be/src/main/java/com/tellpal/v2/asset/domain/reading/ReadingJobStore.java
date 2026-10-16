package com.tellpal.v2.asset.domain.reading;

import java.util.Optional;
import java.util.UUID;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Preparation;

/** Short atomic job operations; callers perform all external work before finalization. */
public interface ReadingJobStore {
    Preparation schedule(ReadingWork work);
    Preparation schedule(ReadingWork work, ReadingDeadline deadline);
    Preparation find(UUID id);
    ReadingWork work(UUID id);
    Optional<ReadingWork.Attempt> claim(UUID id, long nowMillis);
    Optional<UUID> next(long nowMillis);
    Preparation retry(UUID id);
    Preparation succeed(ReadingWork.Attempt attempt, ReadingWork.Manifest manifest, long nowMillis);
    Preparation succeed(ReadingWork.Attempt attempt, ReadingWork.Manifest manifest, long nowMillis, ReadingDeadline deadline);
    Preparation fail(ReadingWork.Attempt attempt, String diagnostic, long nowMillis);
    void evidence(ReadingWork.Attempt attempt, String kind, String detail);
    void integrity(String revision, boolean valid, String detail);
    void planCapture(UUID owner, String path, String provider, String sha256);
    void observeCapture(UUID owner, String path, String version);
    void attachCapture(UUID owner, UUID jobId);
}
