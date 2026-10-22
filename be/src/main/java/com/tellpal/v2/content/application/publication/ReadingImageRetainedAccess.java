package com.tellpal.v2.content.application.publication;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tellpal.v2.asset.api.ReadingPublicationReadyApi;
import com.tellpal.v2.content.api.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Çağıranın kısa transaction'ında aynı ücretsiz retained LIVE erişimini doğrular. */
@Component
class ReadingImageRetainedAccess {
    private final ReadingPublicationMutationGuard guard;
    private final ContentRepository contents;
    private final ContentFreeAccessApi free;
    private final JdbcReadingPublicationStore publications;
    private final ReadingPublicationReadyApi ready;
    ReadingImageRetainedAccess(ReadingPublicationMutationGuard guard, ContentRepository contents,
            ContentFreeAccessApi free, JdbcReadingPublicationStore publications, ReadingPublicationReadyApi ready) {
        this.guard=guard;this.contents=contents;this.free=free;this.publications=publications;this.ready=ready;
    }
    Captured capture(long id, String language) {
        guard.lock(id);
        var content=contents.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QA kopyası bulunamadı"));
        if (!content.isActive() || content.getType()!=ContentType.STORY
                || content.findLocalization(LanguageCode.from(language)).filter(l -> l.getStatus()==LocalizationStatus.PUBLISHED).isEmpty()
                || !free.isContentFree(id, LanguageCode.from(language), null))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ücretsiz editoryal erişim kapalı");
        var row=publications.row(id, language).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LIVE bulunamadı"));
        if (!Boolean.TRUE.equals(row.get("requested")) || !Boolean.TRUE.equals(row.get("live_eligible")) || row.get("live_proof_json")==null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LIVE uygun değil");
        var proof=publications.decode((String) row.get("live_proof_json"), ReadingPublicationReadyApi.Proof.class);
        var identity=new ReadingImageComparisonApi.Identity(id, language, ((Number) row.get("live_generation")).longValue(), proof.revision(), proof.manifestDigest(), proof.jobId());
        if (!ready.retainedEligible(proof))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "LIVE kimliği değişti veya medya geri çekildi");
        return new Captured(identity, proof);
    }
    record Captured(ReadingImageComparisonApi.Identity identity, ReadingPublicationReadyApi.Proof proof) { }
}
