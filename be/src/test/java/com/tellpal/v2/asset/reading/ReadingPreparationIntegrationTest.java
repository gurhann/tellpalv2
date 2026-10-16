package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class ReadingPreparationIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired ReadingJobStore jobs;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.tellpal.v2.content.api.ReadingImageSourceSnapshotApi snapshots;
    @TempDir Path directory;
    ReadingFixtures.DiskStorage disk;
    ReadingImagePreparationService service;
    ReadingImageWorker worker;
    @BeforeEach void setup() throws Exception {
        jdbc.execute("truncate reading_image_integrity,reading_image_attempt_evidence,reading_image_manifests,reading_image_jobs cascade");
        disk=new ReadingFixtures.DiskStorage(directory);
        disk.createImmutable("original/cover","image/png",ReadingFixtures.image(80,60));
        disk.createImmutable("original/page","image/png",ReadingFixtures.image(80,60));
        var assets=mock(MediaAssetRepository.class);
        when(assets.findById(3L)).thenReturn(Optional.of(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,"original/cover",MediaKind.ORIGINAL_IMAGE)));
        when(assets.findById(4L)).thenReturn(Optional.of(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,"original/page",MediaKind.ORIGINAL_IMAGE)));
        var env=new MockEnvironment();env.setActiveProfiles("test");
        var properties=new AssetStorageFirebaseProperties(true,"synthetic","synthetic","unused","test",null,null,null);
        service=new ReadingImagePreparationService(assets,disk.storage(),jobs,env,properties,true);
        worker=new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC());
    }
    @Test void completeStoredRoleSetDeduplicatesAndReverifiesIntegrity() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());worker.run(prepared.jobId());
        var ready=service.find(prepared.jobId());assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);
        assertThat(manifest.outputs()).extracting(o -> o.binding().role()).containsExactly(Role.READING_COVER,Role.READING_DETAIL,Role.READING_PAGE);
        assertThat(service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles())).isEqualTo(ready);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(1);
        assertThat(manifest.outputs().get(2).binding().pageId()).isEqualTo(11L);
        var first=manifest.outputs().getFirst();Files.write(disk.file(first.stored().path()),new byte[]{1});
        assertThatThrownBy(() -> service.find(prepared.jobId())).hasMessageContaining("READY_INTEGRITY_FAILED");
        assertThat(jobs.find(prepared.jobId()).manifest()).isEqualTo(ready.manifest());
        assertThat(jdbc.queryForObject("select count(*) from reading_image_integrity where valid=false",Integer.class)).isEqualTo(1);
    }
    @Test void explicitRetryAndLateTerminalCallbacksRemainFenced() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        var token=jobs.claim(prepared.jobId(),System.currentTimeMillis()).orElseThrow();
        var failed=jobs.fail(token,"simulated interrupted upload",System.currentTimeMillis());
        assertThat(jobs.fail(token,"simulated interrupted upload",token.expiresEpochMillis()+10)).isEqualTo(failed);
        assertThatThrownBy(() -> jobs.fail(token,"contradictory",System.currentTimeMillis())).hasMessageContaining("NON_OWNING");
        jobs.retry(prepared.jobId());var replacement=jobs.claim(prepared.jobId(),System.currentTimeMillis()).orElseThrow();
        assertThat(replacement.number()).isEqualTo(token.number()+1);
        assertThatThrownBy(() -> jobs.fail(token,"late",System.currentTimeMillis())).hasMessageContaining("STALE");
        jobs.fail(replacement,"owned failure",System.currentTimeMillis());service.retry(prepared.jobId());worker.run(prepared.jobId());
        var ready=service.find(prepared.jobId());assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var current=token(prepared.jobId());var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);
        assertThat(jobs.succeed(current,manifest,current.expiresEpochMillis()+1)).isEqualTo(ready);
        assertThatThrownBy(() -> jobs.fail(current,"late failure",System.currentTimeMillis())).hasMessageContaining("NON_OWNING");
        var altered=new ArrayList<>(manifest.outputs());var o=altered.getFirst();
        altered.set(0,new Output(o.binding(),o.sourceSha256(),o.profileFingerprint(),o.renditionKey(),o.provider(),o.stored(),o.sha256(),1,1));
        assertThatThrownBy(() -> jobs.succeed(current,new Manifest(manifest.revision(),manifest.snapshot(),altered),System.currentTimeMillis())).hasMessageContaining("CONTRADICTORY_SUCCESS");
    }
    @Test void concurrentClaimAndExpiredReplacementAllowOnlyOneOwner() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        try(var executor=Executors.newFixedThreadPool(2)) {
            var gate=new CyclicBarrier(2);Callable<Optional<Attempt>> claim=() -> {gate.await();return jobs.claim(prepared.jobId(),System.currentTimeMillis());};
            var a=executor.submit(claim);var b=executor.submit(claim);
            assertThat(List.of(a.get(),b.get()).stream().filter(Optional::isPresent).count()).isEqualTo(1);
        }
        var old=token(prepared.jobId());var newer=jobs.claim(prepared.jobId(),old.expiresEpochMillis()+1).orElseThrow();
        assertThatThrownBy(() -> jobs.fail(old,"old",old.expiresEpochMillis()-1)).hasMessageContaining("STALE");
        jobs.fail(newer,"replacement",newer.expiresEpochMillis()-1);
        assertThat(jobs.find(prepared.jobId()).diagnostic()).isEqualTo("replacement");
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
    }
    @Test void sourceByteLanguageChangesAndInvalidOrProductionInputsFailClosed() throws Exception {
        var first=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());var snapshot=ReadingFixtures.snapshot();
        assertThat(service.prepare(new Snapshot(snapshot.contentId(),"en",snapshot.bindings(),List.of()),ReadingFixtures.profiles()).revision()).isNotEqualTo(first.revision());
        disk.objects.remove("original/page");Files.delete(disk.file("original/page"));disk.createImmutable("original/page","image/png",ReadingFixtures.image(81,60));
        assertThat(service.prepare(snapshot,ReadingFixtures.profiles()).revision()).isNotEqualTo(first.revision());
        assertThatThrownBy(() -> service.prepare(new Snapshot(9,"tr",List.of(),List.of("EMPTY_PAGE_SET")),ReadingFixtures.profiles())).hasMessageContaining("Invalid reading snapshot");
        var env=new MockEnvironment();env.setActiveProfiles("test","production");
        var blocked=new ReadingImagePreparationService(mock(MediaAssetRepository.class),disk.storage(),jobs,env,new AssetStorageFirebaseProperties(true,"x","x","x","test",null,null,null),true);
        assertThatThrownBy(() -> blocked.prepare(snapshot,ReadingFixtures.profiles())).hasMessageContaining("NO_QUALIFIED_PRODUCTION");
    }
    @Test void manifestAndCompletionRollbackTogetherOnDatabaseFailure() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());worker.run(prepared.jobId());
        var ready=jobs.find(prepared.jobId());assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);var work=jobs.work(prepared.jobId());
        jdbc.execute("truncate reading_image_integrity,reading_image_attempt_evidence,reading_image_manifests,reading_image_jobs cascade");
        var pending=jobs.schedule(work);var token=jobs.claim(pending.jobId(),System.currentTimeMillis()).orElseThrow();
        jdbc.execute("create function p2_test_reject_completion() returns trigger language plpgsql as $$ begin if new.status='COMPLETED' then raise exception 'forced rollback'; end if; return new; end; $$");
        jdbc.execute("create trigger p2_test_rollback before update on reading_image_jobs for each row execute function p2_test_reject_completion()");
        try { assertThatThrownBy(() -> jobs.succeed(token,manifest,System.currentTimeMillis())).hasMessageContaining("forced rollback"); }
        finally { jdbc.execute("drop trigger p2_test_rollback on reading_image_jobs");jdbc.execute("drop function p2_test_reject_completion()"); }
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
        assertThat(jobs.find(pending.jobId()).state()).isEqualTo(State.PROCESSING);
        assertThat(jobs.succeed(token,manifest,System.currentTimeMillis()).state()).isEqualTo(State.COMPLETED);
    }
    @Test void contentExactLanguageSnapshotBecomesInvalidAfterLastPageDeletionWhileReadySurvives() throws Exception {
        long contentId=jdbc.queryForObject("insert into contents(type,external_key,page_count) values('STORY',?,1) returning id",Long.class,"p2-"+UUID.randomUUID());
        jdbc.update("insert into content_localizations(content_id,language_code,title,cover_media_id) values(?,'tr','Synthetic',3)",contentId);
        long pageId=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,1) returning id",Long.class,contentId);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,illustration_media_id) values(?,'tr',4)",pageId);
        var captured=snapshots.capture(contentId,"tr");captured.requireComplete();
        assertThat(snapshots.capture(contentId,"en").errors()).contains("EXACT_LANGUAGE_COVER_REQUIRED","EXACT_LANGUAGE_PAGE_REQUIRED:1");
        var job=service.prepare(captured,ReadingFixtures.profiles());worker.run(job.jobId());var ready=service.find(job.jobId());
        assertThat(ready.state()).isEqualTo(State.COMPLETED);
        jdbc.update("delete from story_pages where id=?",pageId);
        assertThat(snapshots.capture(contentId,"tr").errors()).contains("EMPTY_PAGE_SET");
        assertThat(snapshots.matches(captured)).isFalse();
        assertThat(service.find(job.jobId()).manifest()).isEqualTo(ready.manifest());
        jdbc.update("delete from contents where id=?",contentId);
    }
    @Test void restartAfterPartialObjectsReverifiesAndCompletesWithoutPartialReady() throws Exception {
        var job=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        var broken=new com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess() {
            int calls;
            @Override public byte[] convert(byte[] original,TestProfile profile) throws java.io.IOException {
                if(++calls==3) throw new java.io.IOException("injected interruption after cover/detail uploads");
                return super.convert(original,profile);
            }
        };
        new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC(),broken).run(job.jobId());
        assertThat(jobs.find(job.jobId()).state()).isEqualTo(State.FAILED);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reading_image_attempt_evidence where kind='RETAINED_OBJECT'",Integer.class)).isEqualTo(2);
        service.retry(job.jobId());worker.run(job.jobId());
        assertThat(service.find(job.jobId()).state()).isEqualTo(State.COMPLETED);
    }
    @Test void wrongGeometryChildCannotCertifyEvenAValidPersistedPng() throws Exception {
        var job=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        var wrong=new com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess() {
            @Override public byte[] convert(byte[] original,TestProfile profile) throws java.io.IOException { return ReadingFixtures.image(1,1); }
        };
        new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC(),wrong).run(job.jobId());
        assertThat(jobs.find(job.jobId()).state()).isEqualTo(State.FAILED);
        assertThat(jobs.find(job.jobId()).diagnostic()).contains("PROFILE_GEOMETRY_MISMATCH");
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
    }
    @Test void postgresDuplicateScheduleAndSuccessFailureRacePreserveFirstTerminalResult() throws Exception {
        var scheduled=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());worker.run(scheduled.jobId());
        var ready=jobs.find(scheduled.jobId());var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);var work=jobs.work(scheduled.jobId());
        jdbc.execute("truncate reading_image_integrity,reading_image_attempt_evidence,reading_image_manifests,reading_image_jobs cascade");
        try(var executor=Executors.newFixedThreadPool(2)) {
            var gate=new CyclicBarrier(2);Callable<Preparation> schedule=() -> {gate.await();return jobs.schedule(work);};
            var a=executor.submit(schedule);var b=executor.submit(schedule);UUID id=a.get().jobId();assertThat(b.get().jobId()).isEqualTo(id);
            var token=jobs.claim(id,System.currentTimeMillis()).orElseThrow();var terminalGate=new CyclicBarrier(2);
            var success=executor.submit(() -> {terminalGate.await();try {jobs.succeed(token,manifest,System.currentTimeMillis());return true;} catch(ReadingAttemptRejectedException stale){return false;}});
            var failure=executor.submit(() -> {terminalGate.await();try {jobs.fail(token,"race failure",System.currentTimeMillis());return true;} catch(ReadingAttemptRejectedException stale){return false;}});
            assertThat(List.of(success.get(),failure.get()).stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
            var outcome=jobs.find(id);
            assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(outcome.state()==State.COMPLETED?1:0);
        }
    }
    @Test void longFailureEvidenceDoesNotPreventOwnedFailureAndDeduplicatesReplay() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        var token=jobs.claim(prepared.jobId(),System.currentTimeMillis()).orElseThrow();
        String detail="Storage failure: "+"x".repeat(4096);
        jobs.evidence(token,"ATTEMPT_FAILURE",detail);
        jobs.evidence(token,"ATTEMPT_FAILURE",detail);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_attempt_evidence",Integer.class)).isEqualTo(1);
        assertThat(jobs.fail(token,detail,System.currentTimeMillis()).state()).isEqualTo(State.FAILED);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
    }
    @Test void sameBytesWithReorderedPagesOrChangedRoleGeometryCreateDifferentRevisions() throws Exception {
        var onePage=ReadingFixtures.snapshot();
        var original=new Snapshot(onePage.contentId(),onePage.language(),List.of(
                onePage.bindings().get(0),onePage.bindings().get(1),onePage.bindings().get(2),
                new Binding(Role.READING_PAGE,18,12L,2,4)),List.of());
        var baseline=service.prepare(original,ReadingFixtures.profiles());
        var reordered=new Snapshot(original.contentId(),original.language(),List.of(
                original.bindings().get(0),original.bindings().get(1),
                new Binding(Role.READING_PAGE,18,12L,1,4),
                new Binding(Role.READING_PAGE,2,11L,2,4)),List.of());
        var changedOrder=service.prepare(reordered,ReadingFixtures.profiles());
        assertThat(changedOrder.revision()).isNotEqualTo(baseline.revision());
        assertThat(jobs.work(changedOrder.jobId()).sources()).extracting(Source::sha256)
                .containsExactlyElementsOf(jobs.work(baseline.jobId()).sources().stream().map(Source::sha256).toList());
        assertThat(jobs.work(changedOrder.jobId()).profileSet()).isEqualTo(jobs.work(baseline.jobId()).profileSet());

        var changedProfiles=new EnumMap<Role,TestProfile>(Role.class);
        changedProfiles.putAll(ReadingFixtures.profiles());
        changedProfiles.put(Role.READING_PAGE,profileWith(39,true,ReadingFixtures.profile().growthEvidence()));
        var changedGeometry=service.prepare(original,changedProfiles);
        assertThat(changedGeometry.revision()).isNotEqualTo(baseline.revision());
        var before=jobs.work(baseline.jobId());var after=jobs.work(changedGeometry.jobId());
        assertThat(after.sources()).isEqualTo(before.sources());
        assertThat(after.profiles().get(Role.READING_COVER)).isEqualTo(before.profiles().get(Role.READING_COVER));
        assertThat(after.profiles().get(Role.READING_DETAIL)).isEqualTo(before.profiles().get(Role.READING_DETAIL));
        assertThat(after.profiles().get(Role.READING_PAGE).limits().width()).isEqualTo(39);
        assertThat(after.profiles().get(Role.READING_PAGE).fingerprint()).isNotEqualTo(before.profiles().get(Role.READING_PAGE).fingerprint());
    }
    @Test void identicalOriginalBytesAtANewStorageGenerationReuseTheVerifiedRevision() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        worker.run(prepared.jobId());var ready=service.find(prepared.jobId());
        assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var previous=disk.objects.get("original/page");byte[] original=Files.readAllBytes(disk.file(previous.path()));
        Files.write(disk.file(previous.path()),original);
        var replacement=new VersionedStorageObject(previous.path(),UUID.randomUUID().toString(),previous.mime(),previous.bytes());
        disk.objects.put(previous.path(),replacement);
        assertThat(replacement.version()).isNotEqualTo(previous.version());
        assertThat(Files.readAllBytes(disk.file(replacement.path()))).isEqualTo(original);
        var reused=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        assertThat(reused).isEqualTo(ready);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_jobs",Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(1);
    }
    @Test void equalPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete() throws Exception {
        assertPngGrowthAdmission(true);
    }
    @Test void growingPngBytesRequireExplicitGrowthAdmissionBeforeRealWorkerCanComplete() throws Exception {
        assertPngGrowthAdmission(false);
    }
    private void assertPngGrowthAdmission(boolean equalOutput) throws Exception {
        byte[] original;
        if(equalOutput) {
            // A real engine-produced source remains byte-identical after an identity-size PNG pass.
            original=new com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess()
                    .convert(ReadingFixtures.image(80,60),ReadingFixtures.profile());
        } else {
            var solid=new java.awt.image.BufferedImage(80,60,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var encoded=new java.io.ByteArrayOutputStream();
            assertThat(javax.imageio.ImageIO.write(solid,"PNG",encoded)).isTrue();
            original=encoded.toByteArray();
        }
        for(String path:List.of("original/cover","original/page")) {
            disk.objects.remove(path);Files.delete(disk.file(path));disk.createImmutable(path,"image/png",original);
        }
        var deniedProfile=profileWith(40,false,"");
        var deniedProfiles=Map.of(Role.READING_COVER,deniedProfile,Role.READING_DETAIL,deniedProfile,Role.READING_PAGE,deniedProfile);
        var denied=service.prepare(ReadingFixtures.snapshot(),deniedProfiles);worker.run(denied.jobId());
        assertThat(jobs.find(denied.jobId()).state()).isEqualTo(State.FAILED);
        assertThat(jobs.find(denied.jobId()).diagnostic()).contains("OUTPUT_GROWTH_NOT_ADMITTED");
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();

        String evidence="Synthetic local PNG equal/growing-byte admission fixture; no production qualification";
        var admittedProfile=profileWith(40,true,evidence);
        var admittedProfiles=Map.of(Role.READING_COVER,admittedProfile,Role.READING_DETAIL,admittedProfile,Role.READING_PAGE,admittedProfile);
        var admitted=service.prepare(ReadingFixtures.snapshot(),admittedProfiles);
        assertThat(admitted.revision()).isNotEqualTo(denied.revision());
        worker.run(admitted.jobId());var ready=service.find(admitted.jobId());
        assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var deniedWork=jobs.work(denied.jobId());var admittedWork=jobs.work(admitted.jobId());
        assertThat(admittedWork.sourceSet()).isEqualTo(deniedWork.sourceSet());
        assertThat(admittedWork.profileSet()).isNotEqualTo(deniedWork.profileSet());
        var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);
        assertThat(manifest.outputs()).hasSize(3);
        for(int index=0;index<manifest.outputs().size();index++) {
            var output=manifest.outputs().get(index);var source=admittedWork.sources().get(index);
            assertThat(admittedWork.profiles().get(output.binding().role()).limits().growthEvidence()).isEqualTo(evidence);
            if(equalOutput) {
                assertThat(output.stored().bytes()).isEqualTo(source.retained().bytes());
                assertThat(output.sha256()).isEqualTo(source.sha256());
            } else assertThat(output.stored().bytes()).isGreaterThan(source.retained().bytes());
        }
        assertThat(jobs.find(denied.jobId()).state()).isEqualTo(State.FAILED);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(1);
    }
    private static TestProfile profileWith(int width,boolean allowGrowth,String evidence) {
        var p=ReadingFixtures.profile();
        return new TestProfile(width,p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),p.concurrency(),p.encoderThreads(),allowGrowth,evidence);
    }
    private Attempt token(UUID id) {
        var row=jdbc.queryForMap("select * from reading_image_jobs where id=?",id);
        return new Attempt(id,((Number)row.get("attempt")).intValue(),(UUID)row.get("lease"),((java.sql.Timestamp)row.get("lease_expires_at")).getTime(),(String)row.get("source_set"),(String)row.get("profile_set"));
    }
}
