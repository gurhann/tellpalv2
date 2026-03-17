package com.tellpal.v2.content.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tellpal.v2.asset.api.AssetProcessingStatusChangedEvent;
import com.tellpal.v2.content.application.ContentManagementCommands.MarkContentLocalizationProcessingCommand;
import com.tellpal.v2.content.domain.ProcessingStatus;

@Component
class AssetProcessingStatusListener {

    private final ContentManagementService contentManagementService;

    AssetProcessingStatusListener(ContentManagementService contentManagementService) {
        this.contentManagementService = contentManagementService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(AssetProcessingStatusChangedEvent event) {
        contentManagementService.markLocalizationProcessingStatus(new MarkContentLocalizationProcessingCommand(
                event.contentId(),
                event.languageCode(),
                ProcessingStatus.valueOf(event.status().name())));
    }
}
