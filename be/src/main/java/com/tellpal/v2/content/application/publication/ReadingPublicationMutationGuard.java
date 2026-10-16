package com.tellpal.v2.content.application.publication;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.tellpal.v2.content.application.ReadingImageSnapshotAssembler;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Shared lock order: content aggregate, publication scope, source admissions by asset ID, availability. */
@Component
public class ReadingPublicationMutationGuard {
    private final JdbcTemplate jdbc;
    private final ReadingPublicationProperties config;
    private final JdbcReadingPublicationStore store;
    private final ReadingImageSnapshotAssembler assembler;
    public ReadingPublicationMutationGuard(
            JdbcTemplate jdbc,
            ReadingPublicationProperties config,
            JdbcReadingPublicationStore store,
            ReadingImageSnapshotAssembler assembler) {
        this.jdbc = jdbc;
        this.config = config;
        this.store = store;
        this.assembler = assembler;
    }

    public boolean enabled() {
        return config.enabled();
    }

    public void lock(long id) {
        if (!config.enabled()) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("PUBLICATION_MUTATION_TRANSACTION_REQUIRED");
        jdbc.queryForList("select id from contents where id=? for update", id);
    }

    public void changed(Content content, LanguageCode language) {
        if (!config.enabled() || content.getType() != ContentType.STORY) return;
        content.getLocalizations().stream().filter(l -> language == null || l.getLanguageCode().equals(language)) .sorted(java.util.Comparator.comparing(l -> l.getLanguageCode().value())).forEach(l -> store.changed(assembler.assemble(content, l.getLanguageCode().value()), content.isActive() && l.getStatus() == LocalizationStatus.PUBLISHED, config.profiles()));
    }
}
