package com.tellpal.v2.purchase.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.purchase.application.RevenueCatWebhookCommands.ProcessRevenueCatWebhookCommand;

@Service
public class RevenueCatWebhookProcessingService {

    private final RevenueCatWebhookService revenueCatWebhookService;
    private final PurchaseAttributionService purchaseAttributionService;

    public RevenueCatWebhookProcessingService(
            RevenueCatWebhookService revenueCatWebhookService,
            PurchaseAttributionService purchaseAttributionService) {
        this.revenueCatWebhookService = revenueCatWebhookService;
        this.purchaseAttributionService = purchaseAttributionService;
    }

    @Transactional
    public RevenueCatWebhookProcessingResult process(ProcessRevenueCatWebhookCommand command) {
        RevenueCatWebhookResults.RevenueCatWebhookReceipt receipt = revenueCatWebhookService.process(command);
        PurchaseAttributionResult attributionResult = purchaseAttributionService.createSnapshot(receipt.purchaseEventId());
        return new RevenueCatWebhookProcessingResult(
                receipt.purchaseEventId(),
                receipt.status(),
                attributionResult.snapshotId());
    }
}
