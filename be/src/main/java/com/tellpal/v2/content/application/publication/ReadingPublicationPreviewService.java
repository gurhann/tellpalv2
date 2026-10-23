package com.tellpal.v2.content.application.publication;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Admin önizlemesi aynı uygun LIVE kanıtını I/O öncesinde ve sonrasında doğrular. */
@Service
public class ReadingPublicationPreviewService {
    private final ReadingPublicationProperties config;
    private final ReadingPublicationPreviewProperties preview;
    private final ReadingPublicationMutationGuard guard;
    private final ContentRepository contents;
    private final JdbcReadingPublicationStore store;
    private final ReadingPublicationReadyApi ready;
    private final ReadingImageDeliveryApi delivery;
    private final TransactionTemplate tx;
    public ReadingPublicationPreviewService(ReadingPublicationProperties config, ReadingPublicationPreviewProperties preview,
            ReadingPublicationMutationGuard guard, ContentRepository contents, JdbcReadingPublicationStore store,
            ReadingPublicationReadyApi ready, ReadingImageDeliveryApi delivery, PlatformTransactionManager manager) {
        this.config=config;this.preview=preview;this.guard=guard;this.contents=contents;this.store=store;
        this.ready=ready;this.delivery=delivery;this.tx=new TransactionTemplate(manager);
    }
    /** Kaynak adreslerini taşımadan çıktı ölçülerini ve seçilebilir kimliği döndürür. */
    public Manifest manifest(long id, String language) {
        requireConfigured();
        delivery.reserveControl(preview.limits());
        var captured=tx.execute(s -> capture(id, language, null));
        var descriptor=delivery.descriptor(captured.proof());
        var images=new ArrayList<Image>();
        for (var entry : descriptor.images()) {
            var binding=entry.source().binding();var source=entry.source().raster();
            for (var rendition : entry.renditions()) {
                var raster=rendition.raster();
                images.add(new Image(binding.role(), binding.pageNumber(), rendition.renditionKey(), source.mime(),
                        source.byteSize(), source.width(), source.height(), raster.mime(), raster.byteSize(), raster.width(), raster.height()));
            }
        }
        tx.executeWithoutResult(s -> capture(id, language, captured.identity()));
        return new Manifest(captured.identity(), images);
    }
    /** Yalnız istenen aynı LIVE'ın doğrulanmış rendition byte'ını okur. */
    public Payload read(Identity expected, ReadingImageDeliveryApi.Selection selection) {
        requireConfigured();
        if (selection.canonicalSource()) throw new IllegalArgumentException("SOURCE_DELIVERY_FORBIDDEN");
        var captured=tx.execute(s -> capture(expected.contentId(), expected.language(), expected));
        var descriptor=delivery.descriptor(captured.proof());
        var selected=descriptor.images().stream()
                .filter(e -> e.source().binding().role()==selection.role() && e.source().binding().pageNumber()==selection.pageNumber())
                .flatMap(e -> e.renditions().stream()).filter(r -> r.renditionKey().equals(selection.renditionKey()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("UNKNOWN_RENDITION"));
        var bytes=delivery.readRendition(captured.proof(), selection, preview.limits());
        tx.executeWithoutResult(s -> capture(expected.contentId(), expected.language(), expected));
        return new Payload(bytes, selected.raster().mime());
    }
    private void requireConfigured() {
        if (!config.enabled()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Görsel önizlemesi kapalı");
        try { preview.limits(); } catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Görsel önizleme sınırları gerekli");
        }
    }
    private Captured capture(long id, String language, Identity expected) {
        requireConfigured();
        if (!LanguageCode.from(language).value().equals(language)) throw new IllegalArgumentException("EXACT_LANGUAGE_REQUIRED");
        guard.lock(id);
        var content=contents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!content.isActive() || content.getType()!=ContentType.STORY
                || content.findLocalization(LanguageCode.from(language)).filter(l -> l.getStatus()==LocalizationStatus.PUBLISHED).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "İçerik yayına uygun değil");
        var row=store.row(id, language).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Hazır görsel yok"));
        if (!Boolean.TRUE.equals(row.get("requested")) || !Boolean.TRUE.equals(row.get("live_eligible")) || row.get("live_proof_json")==null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hazır görsel yok");
        var proof=store.decode((String) row.get("live_proof_json"), ReadingPublicationReadyApi.Proof.class);
        var identity=new Identity(id,language,((Number) row.get("live_generation")).longValue(),proof.revision(),proof.manifestDigest(),proof.jobId());
        if ((expected!=null && !expected.equals(identity)) || !ready.retainedEligible(proof))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hazır görseller değişti");
        return new Captured(identity, proof);
    }
    public record Identity(long contentId, String language, long liveGeneration, String revision, String manifestDigest, UUID jobId) { }
    public record Image(Role role, int pageNumber, String renditionKey, String sourceMime, long sourceBytes, int sourceWidth,
            int sourceHeight, String mime, long bytes, int width, int height) { }
    public record Manifest(Identity identity, List<Image> images) { public Manifest { images=List.copyOf(images); } }
    public record Payload(byte[] bytes, String mime) { }
    private record Captured(Identity identity, ReadingPublicationReadyApi.Proof proof) { }
}
