package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Optional;

/**
 * Module-facing use cases for the asset processing lifecycle.
 *
 * <p>The API models scheduling, worker lease transitions, completion, failure, and read access to
 * recent processing state.
 */
public interface AssetProcessingApi {

    /**
     * Creates or refreshes a pending processing request for a content localization.
     *
     * <p>Existing pending work is updated in place, while running or completed work is rejected.
     */
    AssetProcessingRecord schedule(AssetProcessingCommands.ScheduleAssetProcessingCommand command);

    /**
     * Starts a pending processing job and acquires its worker lease.
     */
    AssetProcessingRecord start(AssetProcessingCommands.StartAssetProcessingCommand command);

    /**
     * Resets a failed processing job back to pending with refreshed source context.
     */
    AssetProcessingRecord retry(AssetProcessingCommands.RetryAssetProcessingCommand command);

    /**
     * Returns an expired in-flight job back to pending so it can be picked up again.
     */
    AssetProcessingRecord recoverExpiredLease(AssetProcessingCommands.RecoverExpiredAssetProcessingCommand command);

    /**
     * Marks an in-flight processing job as completed.
     */
    AssetProcessingRecord complete(AssetProcessingCommands.CompleteAssetProcessingCommand command);

    /**
     * Marks an in-flight processing job as failed with worker-provided error details.
     */
    AssetProcessingRecord fail(AssetProcessingCommands.FailAssetProcessingCommand command);

    /**
     * Looks up processing state by content localization.
     */
    Optional<AssetProcessingRecord> findByLocalization(Long contentId, com.tellpal.v2.shared.domain.LanguageCode languageCode);

    /**
     * Lists recent processing entries, capped by the implementation for operational safety.
     */
    List<AssetProcessingRecord> listRecent(int limit);
}
