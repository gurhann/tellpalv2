package com.tellpal.v2.asset.infrastructure.processing;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RecoverExpiredAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.StartAssetProcessingCommand;
import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;

@Component
@ConditionalOnProperty(name = "tellpal.asset.processing.poller.enabled", havingValue = "true", matchIfMissing = true)
public class AssetProcessingPoller {

    private final Clock clock;
    private final AssetProcessingRepository assetProcessingRepository;
    private final AssetProcessingApi assetProcessingApi;
    private final ObjectProvider<AssetProcessingJobExecutor> jobExecutorProvider;
    private final int batchSize;

    public AssetProcessingPoller(
            Clock clock,
            AssetProcessingRepository assetProcessingRepository,
            AssetProcessingApi assetProcessingApi,
            ObjectProvider<AssetProcessingJobExecutor> jobExecutorProvider,
            @Value("${tellpal.asset.processing.poller.batch-size:10}") int batchSize) {
        this.clock = clock;
        this.assetProcessingRepository = assetProcessingRepository;
        this.assetProcessingApi = assetProcessingApi;
        this.jobExecutorProvider = jobExecutorProvider;
        this.batchSize = sanitizeBatchSize(batchSize);
    }

    @Scheduled(fixedDelayString = "${tellpal.asset.processing.poller.fixed-delay-ms:5000}")
    void poll() {
        AssetProcessingJobExecutor jobExecutor = jobExecutorProvider.getIfAvailable();
        if (jobExecutor == null) {
            return;
        }

        Instant now = Instant.now(clock);
        recoverExpiredLeases(now);
        processPendingJobs(now, jobExecutor);
    }

    private void recoverExpiredLeases(Instant now) {
        for (AssetProcessing expired : assetProcessingRepository.findExpiredLeases(now, batchSize)) {
            try {
                assetProcessingApi.recoverExpiredLease(new RecoverExpiredAssetProcessingCommand(
                        expired.getContentId(),
                        expired.getLanguageCode()));
            } catch (RuntimeException ignored) {
                // Another worker may already have advanced the job; polling should continue.
            }
        }
    }

    private void processPendingJobs(Instant now, AssetProcessingJobExecutor jobExecutor) {
        for (AssetProcessing pending : assetProcessingRepository.findPendingBefore(now, batchSize)) {
            try {
                jobExecutor.process(assetProcessingApi.start(new StartAssetProcessingCommand(
                        pending.getContentId(),
                        pending.getLanguageCode())));
            } catch (RuntimeException ignored) {
                // Skip conflicted jobs and continue polling the remaining batch.
            }
        }
    }

    private static int sanitizeBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Asset processing poller batch size must be positive");
        }
        return Math.min(batchSize, 100);
    }
}
