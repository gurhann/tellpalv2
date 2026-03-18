package com.tellpal.v2.purchase.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.purchase.application.RevenueCatWebhookCommands.ProcessRevenueCatWebhookCommand;

/**
 * Application service that composes RevenueCat ingest with purchase attribution snapshot creation.
 */
@Service
public class RevenueCatWebhookProcessingService {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookProcessingService.class);

    private final RevenueCatWebhookService revenueCatWebhookService;
    private final PurchaseAttributionService purchaseAttributionService;

    public RevenueCatWebhookProcessingService(
            RevenueCatWebhookService revenueCatWebhookService,
            PurchaseAttributionService purchaseAttributionService) {
        this.revenueCatWebhookService = revenueCatWebhookService;
        this.purchaseAttributionService = purchaseAttributionService;
    }

    /**
     * Processes a RevenueCat webhook and immediately creates its attribution snapshot.
     */
    @Transactional
    public RevenueCatWebhookProcessingResult process(ProcessRevenueCatWebhookCommand command) {
        RevenueCatWebhookResults.RevenueCatWebhookReceipt receipt = revenueCatWebhookService.process(command);
        PurchaseAttributionResult attributionResult = purchaseAttributionService.createSnapshot(receipt.purchaseEventId());
        log.info(
                "revenuecat_webhook_processed purchaseEventId={} ingestStatus={} snapshotId={}",
                receipt.purchaseEventId(),
                receipt.status(),
                attributionResult.snapshotId());
        return new RevenueCatWebhookProcessingResult(
                receipt.purchaseEventId(),
                receipt.status(),
                attributionResult.snapshotId());
    }
}
