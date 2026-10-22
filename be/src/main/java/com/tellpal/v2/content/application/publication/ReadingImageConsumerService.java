package com.tellpal.v2.content.application.publication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.content.api.ReadingImageComparisonApi.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Kısa erişim kesitleri arasında yalnız doğrulanmış rendition okur. */
@Service
public class ReadingImageConsumerService implements ReadingImageConsumerApi {
    private final ReadingImageConsumerProperties config;
    private final ReadingPublicationMutationGuard guard;
    private final ReadingImageRetainedAccess access;
    private final ReadingImageDeliveryApi delivery;
    private final TransactionTemplate tx;
    public ReadingImageConsumerService(ReadingImageConsumerProperties config, ReadingPublicationMutationGuard guard,
            ReadingImageRetainedAccess access, ReadingImageDeliveryApi delivery, PlatformTransactionManager manager) {
        this.config=config;this.guard=guard;this.access=access;this.delivery=delivery;this.tx=new TransactionTemplate(manager);
    }
    private void scope(long id, String language) {
        if (!config.admitted() || !guard.enabled())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Normal görsel tüketicisi kapalı");
        if (id<=0) throw new IllegalArgumentException("INVALID_CONTENT_ID");
        LanguageCode.from(language);
    }
    @Override public void requireConfigured(long id, String language) {
        scope(id,language);delivery.reserveControl(config.limits());
    }
    private ReadingImageRetainedAccess.Captured proof(long id, String language, Identity expected) {
        scope(id,language);
        var captured=access.capture(id,language);
        if (expected!=null && !expected.equals(captured.identity()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"LIVE kimliği değişti");
        return captured;
    }
    @Override public Admission manifest(long id, String language) { return capture(id,language,null); }
    @Override public Admission status(Identity identity) { return capture(identity.contentId(),identity.language(),identity); }
    private Admission capture(long id, String language, Identity expected) {
        return tx.execute(s -> {
            var proof=proof(id,language,expected);
            return new Admission(proof.identity(),delivery.descriptor(proof.proof()),config.leaseMillis());
        });
    }
    @Override public Payload read(Identity identity, ReadingImageDeliveryApi.Selection selection) {
        if (selection.canonicalSource()) throw new IllegalArgumentException("SOURCE_DELIVERY_FORBIDDEN");
        var captured=tx.execute(s -> proof(identity.contentId(),identity.language(),identity));
        var descriptor=delivery.descriptor(captured.proof());
        var selected=descriptor.images().stream()
                .filter(e -> e.source().binding().role()==selection.role() && e.source().binding().pageNumber()==selection.pageNumber())
                .flatMap(e -> e.renditions().stream()).filter(r -> r.renditionKey().equals(selection.renditionKey()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("UNKNOWN_RENDITION"));
        byte[] bytes=delivery.readRendition(captured.proof(),selection,config.limits());
        tx.executeWithoutResult(s -> proof(identity.contentId(),identity.language(),identity));
        return new Payload(bytes,selected.raster().mime());
    }
}
