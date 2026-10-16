package com.tellpal.v2.content.application.publication;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;

/** Durable restart recovery; failures stop until an explicit retry or a new desired generation. */
@Component
public class ReadingPublicationReconciler {
    private final ReadingPublicationProperties config;
    private final JdbcReadingPublicationStore store;
    private final ReadingPublicationService service;
    public ReadingPublicationReconciler(
            ReadingPublicationProperties config,
            JdbcReadingPublicationStore store,
            ReadingPublicationService service) {
        this.config = config;
        this.store = store;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${tellpal.content.reading-publication.poll-delay:5000}")
    public void poll() {
        if (!config.enabled()) return;
        for (var row:store.pending(config.profiles(), service.effectiveProfileIdentity())) service.reconcile(((Number)row.get("content_id")).longValue(), (String)row.get("language_code"));
    }
}
