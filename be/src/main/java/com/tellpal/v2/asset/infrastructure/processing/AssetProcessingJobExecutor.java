package com.tellpal.v2.asset.infrastructure.processing;

import com.tellpal.v2.asset.api.AssetProcessingRecord;

public interface AssetProcessingJobExecutor {

    void process(AssetProcessingRecord assetProcessingRecord);
}
