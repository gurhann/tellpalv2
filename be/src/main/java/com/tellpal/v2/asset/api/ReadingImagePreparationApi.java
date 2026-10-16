package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Internal captured-byte preparation contract. READY is neither current-generation adoption nor access. */
public interface ReadingImagePreparationApi {
    /** Captures actual original bytes, schedules deduplicated work, and freshly verifies any reusable READY. */
    Preparation prepare(Snapshot snapshot, Map<Role, TestProfile> profiles);
    /** Rechecks pinned objects before returning a completed proof; integrity failure preserves historical evidence. */
    Preparation find(UUID jobId);
    /** Explicitly requeues FAILED work; only a newly claimed token may finalize the retry. */
    Preparation retry(UUID jobId);

    /** Required image obligations within the initial exact-language STORY reading scope. */
    enum Role { READING_COVER, READING_DETAIL, READING_PAGE }
    /** Durable preparation lifecycle; COMPLETED is verified media evidence and never publication status. */
    enum State { PENDING, PROCESSING, COMPLETED, FAILED }

    /** Content-owned exact-language scalar bindings, including explicitly invalid snapshots. */
    record Snapshot(long contentId, String language, List<Binding> bindings, List<String> errors) {
        public Snapshot {
            if (contentId <= 0 || !LanguageCode.from(language).value().equals(language))
                throw new IllegalArgumentException("Positive content and exact canonical language required");
            bindings = List.copyOf(bindings);
            errors = List.copyOf(errors);
        }
        /** Rejects incomplete, unordered or inconsistent localized obligations before any source I/O. */
        public void requireComplete() {
            if (!errors.isEmpty() || bindings.size() < 3 || bindings.size() > 102) throw new IllegalArgumentException("Invalid reading snapshot: " + errors);
            if (bindings.get(0).role() != Role.READING_COVER || bindings.get(1).role() != Role.READING_DETAIL)
                throw new IllegalArgumentException("Cover and detail obligations required");
            if(bindings.get(0).assetId()!=bindings.get(1).assetId() || bindings.get(0).localizationId()!=bindings.get(1).localizationId())
                throw new IllegalArgumentException("Cover/detail must bind the same exact localization artwork");
            var ids = new java.util.HashSet<Long>();
            for (int i = 2; i < bindings.size(); i++) {
                Binding b = bindings.get(i);
                if (b.role() != Role.READING_PAGE || b.pageNumber() != i - 1 || !ids.add(b.pageId()))
                    throw new IllegalArgumentException("Complete distinct contiguous ordered pages required");
            }
        }
    }
    /** Historical scalar source provenance; it creates no dependency on mutable page/localization-row lifetime. */
    record Binding(Role role, long localizationId, Long pageId, int pageNumber, long assetId) {
        public Binding {
            if (role == null || localizationId <= 0 || assetId <= 0 ||
                    (role == Role.READING_PAGE ? pageId == null || pageId <= 0 || pageNumber <= 0 : pageId != null || pageNumber != 0))
                throw new IllegalArgumentException("Invalid localized role binding");
        }
    }
    /** Explicit synthetic/local admission. No production profile or production admission exists in P2. */
    record TestProfile(int width, int height, long maxSourceBytes, long maxOutputBytes, long maxPixels,
            long parentMemoryBytes, int heapMiB, int directMiB, int processMiB, long diskBytes,
            long sourceMillis, long revisionMillis, int concurrency, int encoderThreads,
            boolean allowGrowth, String growthEvidence) {
        public TestProfile {
            if (width <= 0 || height <= 0 || maxSourceBytes <= 0 || maxSourceBytes > Integer.MAX_VALUE - 8L
                    || maxOutputBytes <= 0 || maxOutputBytes > Integer.MAX_VALUE - 8L || maxPixels <= 0
                    || parentMemoryBytes <= 0 || heapMiB < 32 || directMiB < 1 || processMiB < heapMiB + directMiB
                    || diskBytes < maxSourceBytes + maxOutputBytes || sourceMillis < 1 || sourceMillis > Integer.MAX_VALUE || revisionMillis < sourceMillis
                    || concurrency != 1 || encoderThreads != 1 || growthEvidence == null
                    || (allowGrowth && growthEvidence.isBlank())) throw new IllegalArgumentException("Invalid explicit test resource/admission profile");
            long raster = Math.multiplyExact(maxPixels, 16L);
            if (Math.multiplyExact(concurrency, Math.addExact(raster, Math.multiplyExact(maxSourceBytes + maxOutputBytes, 4L))) > parentMemoryBytes)
                throw new IllegalArgumentException("Parent full-raster/encoded memory admission exceeded");
            if (raster > heapMiB * 1024L * 1024L / 2) throw new IllegalArgumentException("Child full-raster admission exceeded");
        }
    }
    /** Captured revision/job proof without a URL, access grant or desired-publication-generation assertion. */
    record Preparation(UUID jobId, String revision, State state, String manifestDigest, String manifest, String diagnostic) { }
}
