package com.tellpal.v2.asset.application.reading;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import com.tellpal.v2.asset.infrastructure.storage.ReadingObjectStorage;

/** Fresh external verification reserves a DB epoch before I/O; late observations cannot undo withdrawal/failure. */
@Service
public class ReadingPublicationReadyService implements ReadingPublicationReadyApi {
    private final ReadingImagePreparationService preparation;
    private final ReadingSourceAdmissionService admissions;
    private final ReadingJobStore jobs;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ReadingObjectStorage storage;
    public ReadingPublicationReadyService(
            ReadingImagePreparationService preparation,
            ReadingSourceAdmissionService admissions,
            ReadingJobStore jobs,
            JdbcTemplate jdbc,
            PlatformTransactionManager manager,
            ReadingObjectStorage storage) {
        this.preparation = preparation;
        this.admissions = admissions;
        this.jobs = jobs;
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(manager);
        this.storage = storage;
    }

    @Override
    public String effectiveProfileIdentity(Map<Role, TestProfile> profiles) {
        preparation.requireTestAdmission();
        ReadingPreparationCapacity.requireNoTransaction();
        var fingerprints = new TreeMap<String, String>();
        profiles.forEach((role, limits) -> fingerprints.put(role.name(),
                ReadingProfiles.capture(role, limits).fingerprint()));
        return ReadingIdentity.hash("tellpal.reading.profile-set.v1", fingerprints);
    }

    @Override
    public Preparation prepareAdmitted(Snapshot snapshot, Map<Role, TestProfile> profiles) {
        return preparation.prepareAdmitted(snapshot, profiles);
    }

    @Override
    public Preparation retry(UUID id) {
        return preparation.retry(id);
    }

    @Override
    public Preparation state(UUID id) {
        preparation.requireTestAdmission();
        return jobs.find(id);
    }

    @Override
    public Proof verify(UUID id, Map<Role, TestProfile> profiles) {
        preparation.requireTestAdmission();
        ReadingPreparationCapacity.requireNoTransaction();
        var deadline = ReadingDeadline.start(
                profiles.values().stream().mapToLong(TestProfile::revisionMillis).min().orElseThrow());
        var work = jobs.work(id);
        deadline.check();
        if (!profiles.keySet().equals(work.profiles().keySet()) || profiles.entrySet().stream()
                .anyMatch(e -> !ReadingProfiles.capture(e.getKey(), e.getValue()).equals(work.profiles().get(e.getKey())))) {
            throw new IllegalStateException("CURRENT_PROFILE_CHANGED");
        }
        deadline.check();
        var epochs = work.sources().stream().map(source -> {
            if (source.admissionId() == null || source.admissionEpoch() == null) throw new IllegalStateException("SOURCE_NOT_ADMITTED");
            return new SourceEpoch(source.binding().assetId(), source.admissionId(), source.admissionEpoch());
        }).distinct().sorted(Comparator.comparingLong(SourceEpoch::assetId)).toList();
        long epoch = tx.execute(s -> {
            jdbc.update("""
                insert into reading_publication_availability(revision)
                values(?)
                on conflict do nothing
                """, work.revision());
            return jdbc.queryForObject("""
                update reading_publication_availability
                set epoch=epoch+1
                where revision=?
                returning epoch
                """, Long.class, work.revision());
        });
        try (var capacity = ReadingPreparationCapacity.acquire()) {
            work.profiles().values().forEach(profile -> ReadingPreparationCapacity.requireParentMemory(profile.limits()));
            for (var source : work.sources()) {
                deadline.check();
                var authority = admissions.require(source.binding().assetId(), false);
                var profile = work.profiles().get(source.binding().role()).limits();
                var bytes = storage.read(source.provider(), admissions.object(authority), profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
                deadline.check();
                var decoded = ReadingImageInspector.decode(bytes, profile);
                if (!decoded.mime().equals(admissions.object(authority).mime()) || !decoded.mime().equals(source.retained().mime())) throw new IllegalStateException("SOURCE_STORAGE_MIME_MISMATCH");
                deadline.check();
                if (!ReadingIdentity.sha(bytes).equals(source.sha256())) throw new IllegalStateException("ADMITTED_SOURCE_INTEGRITY_FAILED");
            }
        } catch (java.io.IOException | RuntimeException invalid) {
            if (ReadingVerificationInconclusiveException.causedByResourceRejection(invalid)) {
                throw new IllegalStateException("READING_VERIFICATION_INCONCLUSIVE", invalid);
            }
            jobs.integrity(work.revision(), false, "ADMITTED_SOURCE_INTEGRITY_FAILED");
            throw new IllegalStateException("ADMITTED_SOURCE_INTEGRITY_FAILED", invalid);
        }
        Preparation result = preparation.find(id, deadline);
        if (result.state() != State.COMPLETED) throw new IllegalStateException("READING_NOT_READY");
        var proof = new Proof(id, result.revision(), result.manifestDigest(), epoch, work.snapshot(), epochs, profiles);
        boolean valid = tx.execute(s -> {
            deadline.check();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    deadline.check();
                }
            });
            if (!sources(proof)) return false;
            return jdbc.update("""
                update reading_publication_availability
                set valid=true,checked_at=now()
                where revision=?
                and epoch=?
                """, proof.revision(), epoch) == 1;
        });
        if (!valid) throw new IllegalStateException("STALE_AVAILABILITY_OBSERVATION");
        deadline.check();
        return proof;
    }

    @Override
    public boolean guard(Proof proof, Snapshot current) {
        preparation.requireTestAdmission();
        requireTransaction();
        return proof.snapshot().equals(current) && check(proof, true);
    }

    @Override
    public boolean retainedEligible(Proof proof) {
        preparation.requireTestAdmission();
        requireTransaction();
        return check(proof, false);
    }

    private boolean check(Proof proof, boolean exactEpoch) {
        var work = jobs.work(proof.jobId());
        var expected = work.sources().stream().map(source -> new SourceEpoch(
                source.binding().assetId(), source.admissionId(), source.admissionEpoch() == null ? 0 : source.admissionEpoch()))
                .distinct().sorted(Comparator.comparingLong(SourceEpoch::assetId)).toList();
        if (!work.profiles().keySet().equals(proof.profiles().keySet()) || work.profiles().entrySet().stream()
                .anyMatch(e -> !e.getValue().limits().equals(proof.profiles().get(e.getKey())))) return false;
        if (!work.revision().equals(proof.revision()) || !work.snapshot().equals(proof.snapshot())
                || !expected.equals(proof.sources()) || !sources(proof)) return false;
        var rows = jdbc.queryForList("""
                select a.epoch,a.valid,j.status,j.manifest_digest from reading_publication_availability a
                join reading_image_jobs j on j.revision=a.revision
                where a.revision=?
                and j.id=? for update of a
                """, proof.revision(), proof.jobId());
        if (rows.size() != 1) return false;
        var row = rows.getFirst();
        return Boolean.TRUE.equals(row.get("valid"))
                && (!exactEpoch || ((Number) row.get("epoch")).longValue() == proof.availabilityEpoch())
                && "COMPLETED".equals(row.get("status"))
                && proof.manifestDigest().equals(row.get("manifest_digest"));
    }

    private boolean sources(Proof proof) {
        for (var source : proof.sources().stream().sorted(Comparator.comparingLong(SourceEpoch::assetId)).toList()) {
            try {
                var row = admissions.require(source.assetId(), true);
                if (!source.admissionId().equals(row.get("id")) || source.epoch() != ((Number)row.get("epoch")).longValue()) return false;
            } catch (ReadingSourceNotAdmittedException invalid) {
                return false;
            }
        }
        return !proof.sources().isEmpty();
    }

    private static void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("ADOPTION_TRANSACTION_REQUIRED");
    }
}
