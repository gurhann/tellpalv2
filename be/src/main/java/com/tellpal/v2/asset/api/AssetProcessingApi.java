package com.tellpal.v2.asset.api;

import java.util.List;
import java.util.Optional;

public interface AssetProcessingApi {

    AssetProcessingRecord schedule(AssetProcessingCommands.ScheduleAssetProcessingCommand command);

    AssetProcessingRecord start(AssetProcessingCommands.StartAssetProcessingCommand command);

    AssetProcessingRecord retry(AssetProcessingCommands.RetryAssetProcessingCommand command);

    AssetProcessingRecord recoverExpiredLease(AssetProcessingCommands.RecoverExpiredAssetProcessingCommand command);

    AssetProcessingRecord complete(AssetProcessingCommands.CompleteAssetProcessingCommand command);

    AssetProcessingRecord fail(AssetProcessingCommands.FailAssetProcessingCommand command);

    Optional<AssetProcessingRecord> findByLocalization(Long contentId, com.tellpal.v2.shared.domain.LanguageCode languageCode);

    List<AssetProcessingRecord> listRecent(int limit);
}
