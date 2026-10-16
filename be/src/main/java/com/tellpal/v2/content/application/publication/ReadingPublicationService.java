package com.tellpal.v2.content.application.publication;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.content.api.ReadingPublicationApi;
import com.tellpal.v2.content.application.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.domain.publication.ReadingPublication;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Durable exact-language intent and automatic adoption. External media operations never join these short transactions. */
@Service
public class ReadingPublicationService implements ReadingPublicationApi {
    private final ReadingPublicationProperties config;
    private final ReadingPublicationMutationGuard guard;
    private final ContentRepository contents;
    private final JdbcReadingPublicationStore store;
    private final ReadingImageSnapshotAssembler assembler;
    private final ReadingPublicationReadyApi ready;
    private final TransactionTemplate tx;
    public ReadingPublicationService(
            ReadingPublicationProperties config,
            ReadingPublicationMutationGuard guard,
            ContentRepository contents,
            JdbcReadingPublicationStore store,
            ReadingImageSnapshotAssembler assembler,
            ReadingPublicationReadyApi ready,
            PlatformTransactionManager manager) {
        this.config = config;
        this.guard = guard;
        this.contents = contents;
        this.store = store;
        this.assembler = assembler;
        this.ready = ready;
        this.tx = new TransactionTemplate(manager);
    }

    /** Called by Publish while the content lock and editorial write transaction are held. */
    public void request(Content content, LanguageCode language) {
        if (!config.enabled() || content.getType() != ContentType.STORY) return;
        store.request(assembler.assemble(content, language.value()), config.profiles());
    }

    @Override
    public Status status(long id, String language) {
        exact(language);
        if (!config.enabled()) return new Status(false, "INTERNAL_LOCAL_TEST", "DISABLED", null, null, null, false, null, null, id, language);
        return tx.execute(s -> {
            var content = locked(id);
            var row = store.row(id, language);
            if (row.isEmpty()) return new Status(true, "INTERNAL_LOCAL_TEST", "NOT_REQUESTED", null, null, null, false, null, null, id, language);
            var r = row.get();
            String live = null;
            String digest = null;
            Long liveGeneration = null;
            if (eligibleRetained(content, language) && Boolean.TRUE.equals(r.get("requested")) && Boolean.TRUE.equals(r.get("live_eligible")) && r.get("live_proof_json") != null) {
                var proof = store.decode((String)r.get("live_proof_json"), ReadingPublicationReadyApi.Proof.class);
                if (ready.retainedEligible(proof)) {
                    live = proof.revision();
                    digest = proof.manifestDigest();
                    liveGeneration = ((Number)r.get("live_generation")).longValue();
                }
            }
            String state = (String)r.get("state");
            if (state.equals("LIVE") && live == null) {
                store.unavailable(id, language);
                state = "FAILED";
            }
            String diagnostic = (String)r.get("diagnostic");
            if (diagnostic == null && state.equals("FAILED")) diagnostic = "RETAINED_MEDIA_UNAVAILABLE";
            return new Status(true, "INTERNAL_LOCAL_TEST", state, ((Number)r.get("generation")).longValue(), live, diagnostic, state.equals("FAILED") && Boolean.TRUE.equals(r.get("requested")), liveGeneration, digest, id, language);
        });
    }

    @Override
    public Status retry(long id, String language) {
        exact(language);
        if (!config.enabled()) return status(id, language);
        status(id, language);
        tx.executeWithoutResult(s -> {
            locked(id);
            store.retry(id, language);
        });
        return status(id, language);
    }

    /** Claims one durable scope and reconciles it without holding DB locks during preparation or verification. */
    public void reconcile(long id, String language) {
        if (!config.enabled()) return;
        var profiles = config.profiles();
        var effectiveIdentity = ready.effectiveProfileIdentity(profiles);
        var claim = tx.execute(s -> {
            locked(id);
            store.refreshProfiles(id, language, profiles, effectiveIdentity);
            return store.claim(id, language);
        });
        if (claim.isEmpty()) return;
        var token = claim.get();
        try {
            token.snapshot().requireComplete();
            Preparation result;
            if (token.jobId() == null) result = ready.prepareAdmitted(token.snapshot(), profiles);
            else result = ready.state(token.jobId());
            if (result.state() == State.FAILED) {
                if (token.explicitRetry()) result = ready.retry(result.jobId());
                else {
                    fail(token, "READING_PREPARATION_FAILED");
                    return;
                }
            }
            if (result.state() != State.COMPLETED) {
                UUID job = result.jobId();
                tx.executeWithoutResult(s -> {
                    locked(id);
                    store.attach(token, job);
                });
                return;
            }
            var proof = ready.verify(result.jobId(), profiles);
            var currentProfiles = config.profiles();
            String currentIdentity = config.enabled() ? ready.effectiveProfileIdentity(currentProfiles) : null;
            tx.executeWithoutResult(s -> {
                if (!config.enabled()) return;
                var content = locked(id);
                if (!store.owns(token)) return;
                if (!config.profiles().equals(profiles) || !Objects.equals(token.effectiveProfileIdentity(), currentIdentity)) {
                    store.refreshProfiles(id, language, currentProfiles, currentIdentity);
                    return;
                }
                var current = assembler.assemble(content, language);
                if (!current.equals(token.snapshot()) || !eligibleNew(content, language)) {
                    store.fail(token, "CURRENT_CONTENT_INELIGIBLE");
                    return;
                }
                if (!ready.guard(proof, current)) {
                    store.fail(token, "STALE_MEDIA_PROOF");
                    return;
                }
                store.adopt(token, proof);
            });
        } catch (RuntimeException invalid) {
            fail(token, diagnostic(invalid));
        }
    }

    /** Scheduler selector uses the asset-owned effective identity without opening a DB transaction. */
    public String effectiveProfileIdentity() {
        return ready.effectiveProfileIdentity(config.profiles());
    }

    private void fail(ReadingPublication token, String diagnostic) {
        tx.executeWithoutResult(s -> {
            locked(token.contentId());
            store.fail(token, diagnostic);
        });
    }

    private Content locked(long id) {
        guard.lock(id);
        return contents.findById(id).orElseThrow(() -> new ContentApplicationExceptions.ContentNotFoundException(id));
    }

    private static boolean eligibleRetained(Content content, String language) {
        return content.isActive() && content.getType() == ContentType.STORY && content.findLocalization(LanguageCode.from(language)) .map(l -> l.getStatus() == LocalizationStatus.PUBLISHED).orElse(false);
    }

    private static boolean eligibleNew(Content content, String language) {
        return eligibleRetained(content, language) && new StoryPublicationReadinessPolicy().evaluate(content, LanguageCode.from(language)).isEmpty();
    }

    private static void exact(String language) {
        if (!LanguageCode.from(language).value().equals(language)) throw new IllegalArgumentException("EXACT_LANGUAGE_REQUIRED");
    }

    private static String diagnostic(RuntimeException e) {
        String message = Objects.toString(e.getMessage(), "");
        if (message.contains("SOURCE_NOT_ADMITTED")) return "SOURCE_NOT_ADMITTED";
        if (message.contains("Invalid reading snapshot")) return "INVALID_READING_OBLIGATIONS";
        if (message.contains("INTEGRITY") || message.contains("STORAGE") || message.contains("PINNED")) return "MEDIA_INTEGRITY_FAILED";
        if (message.contains("SOURCE_SHORTAGE")) return "SOURCE_DIMENSIONS_INSUFFICIENT";
        return "READING_PREPARATION_FAILED";
    }
}
