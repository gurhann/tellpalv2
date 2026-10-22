package com.tellpal.v2.content.application.publication;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Kısa content→publication→asset kesitleri arasında transaction dışı bounded teslimat. */
@Service
public class ReadingImageComparisonService implements ReadingImageComparisonApi {
    private final ReadingImageComparisonProperties config;
    private final ReadingPublicationMutationGuard guard;
    private final ContentRepository contents;
    private final ContentFreeAccessApi free;
    private final JdbcReadingPublicationStore publications;
    private final ReadingPublicationReadyApi ready;
    private final ReadingImageDeliveryApi delivery;
    private final TransactionTemplate tx;
    public ReadingImageComparisonService(ReadingImageComparisonProperties config, ReadingPublicationMutationGuard guard,
            ContentRepository contents, ContentFreeAccessApi free, JdbcReadingPublicationStore publications,
            ReadingPublicationReadyApi ready, ReadingImageDeliveryApi delivery, PlatformTransactionManager manager) {
        this.config=config; this.guard=guard; this.contents=contents; this.free=free;
        this.publications=publications; this.ready=ready; this.delivery=delivery; this.tx=new TransactionTemplate(manager);
    }
    @Override public void requireConfigured(long id,String language) {
        requireScope(id,language);delivery.reserveControl(config.limits());
    }
    private void requireScope(long id,String language) {
        if (!config.admitted() || !guard.enabled() || id != config.contentId() || !Objects.equals(language, config.language()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QA kapsamı veya çalışma ayarları kapalı");
    }
    @Override public Admission manifest(long id, String language) { return capture(id, language, null); }
    @Override public Admission status(Identity identity) { return capture(identity.contentId(), identity.language(), identity); }
    @Override public byte[] read(Identity identity, ReadingImageDeliveryApi.Selection selection) {
        var proof = tx.execute(s -> proof(identity.contentId(), identity.language(), identity).proof());
        byte[] bytes = delivery.read(proof, selection, config.limits());
        tx.executeWithoutResult(s -> proof(identity.contentId(), identity.language(), identity));
        return bytes;
    }
    private Admission capture(long id, String language, Identity expected) {
        return tx.execute(s -> {
            var captured=proof(id, language, expected);
            var descriptor=delivery.descriptor(captured.proof());
            if (descriptor.images().stream().anyMatch(e -> !e.source().raster().mime().equals("image/png")))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Canonical PNG kaynak gerekli");
            return new Admission(captured.identity(), descriptor, config.leaseMillis());
        });
    }
    private Captured proof(long id, String language, Identity expected) {
        requireScope(id, language);
        var captured = new ReadingImageRetainedAccess(guard, contents, free, publications, ready).capture(id, language);
        if (captured.proof().snapshot().bindings().size()!=4)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tam iki sayfalı QA kopyası gerekli");
        if (expected!=null && !expected.equals(captured.identity()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"LIVE kimliği değişti");
        return new Captured(captured.identity(),captured.proof());
    }
    private record Captured(Identity identity, ReadingPublicationReadyApi.Proof proof) { }
}
