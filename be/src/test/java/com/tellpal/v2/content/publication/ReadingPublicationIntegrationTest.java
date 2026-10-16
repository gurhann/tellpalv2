package com.tellpal.v2.content.publication;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.persistence.reading.*;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.application.*;
import com.tellpal.v2.content.application.ContentManagementCommands.*;
import com.tellpal.v2.content.application.ContentPublicationCommands.*;
import com.tellpal.v2.content.application.publication.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.content.infrastructure.persistence.publication.*;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest(properties={"tellpal.content.reading-publication.poll-delay=3600000"})
class ReadingPublicationIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ReadingJobStore jobs;
    @Autowired MediaAssetRepository assets;
    @Autowired ContentRepository contents;
    @Autowired com.tellpal.v2.content.api.PublicContentQueryApi publicContents;
    @Autowired com.tellpal.v2.content.api.ContentPreviewQueryApi listingPreviews;
    @Autowired LibraryCoverQueryApi listingCovers;
    @Autowired StoryPageMediaManifestApi experiment;
    @Autowired JdbcReadingSourceAdmissionStore admissionStore;
    @Autowired JdbcReadingPublicationStore store;
    @Autowired ReadingImageSnapshotAssembler assembler;
    @Autowired ReadingPublicationProperties config;
    @Autowired ReadingPublicationMutationGuard guard;
    ContentPublicationService publishing;
    @Autowired ContentManagementService management;
    @Autowired StoryPageManagementService pages;
    @TempDir Path directory;
    ReadingFixtures.DiskStorage disk;
    ReadingSourceAdmissionService admissions;
    ReadingImagePreparationService preparation;
    ReadingPublicationReadyService ready;
    ReadingPublicationService publication;
    ReadingImageWorker worker;
    TransactionTemplate tx;
    long id,cover,page,legacy;
    String external;
    static final LanguageCode TR=LanguageCode.from("tr");
    @BeforeEach void setup() throws Exception {
        jdbc.execute("truncate reading_publications,reading_source_admissions,reading_image_jobs cascade");
        tx=new TransactionTemplate(manager);
        disk=new ReadingFixtures.DiskStorage(directory);
        var env=new MockEnvironment();env.setActiveProfiles("test");
        var props=new AssetStorageFirebaseProperties(true,"synthetic","synthetic","unused","test",null,null,null);
        admissions=new ReadingSourceAdmissionService(assets,disk.storage(),admissionStore,props,env,true);
        preparation=new ReadingImagePreparationService(assets,disk.storage(),jobs,env,props,true, admissions);
        ready=new ReadingPublicationReadyService(preparation,admissions,jobs,jdbc,manager,disk.storage());
        config.setEnabled(true);config.setProfiles(ReadingFixtures.profiles());
        publication=new ReadingPublicationService(config,guard,contents,store,assembler,ready,manager);
        publishing=new ContentPublicationService(contents,mock(AssetProcessingApi.class),publication,guard);
        worker=new ReadingImageWorker(preparation,jobs,disk.storage(),Clock.systemUTC());
        legacy=original("cover",80);cover=admissions.admit(legacy,ReadingFixtures.profile()).assetId();
        page=admissions.admit(original("page",81),ReadingFixtures.profile()).assetId();
        external="p3-"+UUID.randomUUID();
        id=jdbc.queryForObject("insert into contents(type,external_key,page_count,is_active) values('STORY',?,1,true) returning id",Long.class,external);
        jdbc.update("insert into content_localizations(content_id,language_code,title,description,cover_media_id,status,processing_status) values(?,'tr','Synthetic','Editorial description',?,'DRAFT','COMPLETED')",id,cover);
        long pageId=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,1) returning id",Long.class,id);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr','Page text',?,?)",pageId,legacy,page);
    }
    @AfterEach void disabled() { config.setEnabled(false); }
    long original(String name,int width) throws Exception {
        String path="test/manual/"+name+"-"+UUID.randomUUID();
        disk.createImmutable(path,"image/png",ReadingFixtures.image(width,60));
        return tx.execute(s->assets.save(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,path,MediaKind.ORIGINAL_IMAGE)).getId());
    }
    void publish() { tx.executeWithoutResult(s->publishing.publishLocalization(new PublishContentLocalizationCommand(id,TR,Instant.now()))); }
    UUID job() { return jdbc.queryForObject("select job_id from reading_publications where content_id=?",UUID.class,id); }
    String live() { return publication.status(id,"tr").liveRevision(); }
    void complete() { publication.reconcile(id,"tr");worker.run(job());publication.reconcile(id,"tr");assertThat(publication.status(id,"tr").state()).isEqualTo("LIVE"); }
    void edit(long source,String title) {
        management.updateLocalization(new UpdateContentLocalizationCommand(id,TR,title,"Editorial description",null,source,null,5,LocalizationStatus.PUBLISHED,ProcessingStatus.COMPLETED,Instant.now()));
    }
    @Test void publishCommitsIntentThenActualStoredBytesAreAutomaticallyAdoptedAndDuplicatesAreIdempotent() throws Exception {
        publish();assertThat(live()).isNull();long generation=publication.status(id,"tr").generation();publish();assertThat(publication.status(id,"tr").generation()).isEqualTo(generation);
        complete();String revision=live();publish();publication.reconcile(id,"tr");assertThat(live()).isEqualTo(revision);
        var work=jobs.work(job());var manifest=new ObjectMapper().readValue(jobs.find(job()).manifest(),ReadingWork.Manifest.class);
        assertThat(work.sources()).allMatch(s->s.admissionId()!=null);
        assertThat(manifest.outputs()).hasSize(3);
        for(var output:manifest.outputs()) assertThat(javax.imageio.ImageIO.read(disk.file(output.stored().path()).toFile()).getWidth()).isEqualTo(output.width());
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(1);
    }
    @Test void failedLegacyReplacementRetainsLiveAndExplicitRetryDoesNotSpin() {
        publish();complete();String revision=live();long liveGeneration=publication.status(id,"tr").liveGeneration();edit(legacy,"Replacement");publication.reconcile(id,"tr");
        var failed=publication.status(id,"tr");assertThat(failed.state()).isEqualTo("FAILED");assertThat(failed.diagnostic()).isEqualTo("SOURCE_NOT_ADMITTED");assertThat(failed.generation()).isGreaterThan(liveGeneration);assertThat(failed.liveGeneration()).isEqualTo(liveGeneration);assertThat(failed.liveManifestDigest()).matches("[0-9a-f]{64}");assertThat(live()).isEqualTo(revision);
        publication.retry(id,"tr");assertThat(publication.status(id,"tr").state()).isEqualTo("PENDING");publication.reconcile(id,"tr");
        assertThat(publication.status(id,"tr").state()).isEqualTo("FAILED");assertThat(store.pending(config.profiles(), ready.effectiveProfileIdentity(config.profiles()))).isEmpty();
    }
    @Test void metadataOnlyEditReusesJobAndLastPageRemovalExplicitlyInvalidatesWhileRetainingLive() {
        publish();complete();UUID prior=job();String revision=live();edit(cover,"Updated title");publication.reconcile(id,"tr");assertThat(job()).isEqualTo(prior);assertThat(live()).isEqualTo(revision);
        pages.removeStoryPage(new RemoveStoryPageCommand(id,1));publication.reconcile(id,"tr");
        assertThat(publication.status(id,"tr").diagnostic()).isEqualTo("INVALID_READING_OBLIGATIONS");assertThat(live()).isEqualTo(revision);
    }
    @Test void archiveThenChangedSourceAndNewPublishCannotReopenHistoricalLive() throws Exception {
        publish();complete();String prior=live();tx.executeWithoutResult(s->publishing.archiveLocalization(new ArchiveContentLocalizationCommand(id,TR)));assertThat(live()).isNull();
        long next=admissions.admit(original("new",82),ReadingFixtures.profile()).assetId();edit(next,"New publication");
        assertThat(live()).isNull();publish();assertThat(publication.status(id,"tr").state()).isEqualTo("PENDING");assertThat(live()).isNull();
        complete();assertThat(live()).isNotEqualTo(prior);
    }
    @Test void deactivationAndManualFlagsCannotManufactureLive() {
        management.markAsReady(id,TR);assertThat(live()).isNull();publish();complete();
        management.deleteContent(new DeleteContentCommand(id));assertThat(live()).isNull();
        management.updateContent(new UpdateContentCommand(id,external,5,true));management.markAsReady(id,TR);assertThat(live()).isNull();
    }
    @Test void staleClaimAndForgedSnapshotSourcesOrProfilesCannotAdopt() {
        publish();publication.reconcile(id,"tr");worker.run(job());var proof=ready.verify(job(),config.profiles());
        tx.executeWithoutResult(s->{guard.lock(id);assertThat(ready.guard(proof,proof.snapshot())).isTrue();
            var forged=new ReadingPublicationReadyApi.Proof(proof.jobId(),proof.revision(),proof.manifestDigest(),proof.availabilityEpoch(),new Snapshot(id,"en",proof.snapshot().bindings(),List.of()),proof.sources(),proof.profiles());
            assertThat(ready.guard(forged,forged.snapshot())).isFalse();
            var missing=new ReadingPublicationReadyApi.Proof(proof.jobId(),proof.revision(),proof.manifestDigest(),proof.availabilityEpoch(),proof.snapshot(),List.of(),proof.profiles());
            assertThat(ready.guard(missing,missing.snapshot())).isFalse();
        });
        var token=tx.execute(s->{guard.lock(id);return store.claim(id,"tr").orElseThrow();});
        edit(cover,"Generation changed");tx.executeWithoutResult(s->{guard.lock(id);assertThat(store.owns(token)).isFalse();store.adopt(token,proof);});assertThat(live()).isNull();
    }
    @Test void restartAfterSchedulingAndReadyRecoversAndConcurrentAdoptersCommitOneLive() throws Exception {
        publish();publication.reconcile(id,"tr");UUID scheduled=job();worker.run(scheduled);
        var restarted=new ReadingPublicationService(config,guard,contents,store,assembler,ready,manager);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var gate=new CyclicBarrier(2);Callable<Void> task=()->{gate.await();restarted.reconcile(id,"tr");return null;};
            var a=executor.submit(task);var b=executor.submit(task);a.get();b.get();
        }
        assertThat(publication.status(id,"tr").state()).isEqualTo("LIVE");assertThat(job()).isEqualTo(scheduled);
    }
    @Test void profileChangeAfterReadyAdvancesGenerationAndCannotAdoptOldProfile() {
        publish();publication.reconcile(id,"tr");UUID previous=job();worker.run(previous);long generation=publication.status(id,"tr").generation();
        var p=ReadingFixtures.profile();var smaller=new TestProfile(39,p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,p.allowGrowth(),p.growthEvidence());
        config.setProfiles(Map.of(Role.READING_COVER,p,Role.READING_DETAIL,p,Role.READING_PAGE,smaller));
        publication.reconcile(id,"tr");assertThat(job()).isNotEqualTo(previous);assertThat(live()).isNull();assertThat(publication.status(id,"tr").generation()).isGreaterThan(generation);
        assertThatThrownBy(()->ready.verify(previous,config.profiles())).hasMessageContaining("CURRENT_PROFILE_CHANGED");worker.run(job());publication.reconcile(id,"tr");assertThat(live()).isNotNull();
    }
    @Test void integrityLossRetryRequiresFreshSuccessfulVerificationAndSourceWithdrawalFencesProof() throws Exception {
        publish();complete();var manifest=new ObjectMapper().readValue(jobs.find(job()).manifest(),ReadingWork.Manifest.class);var output=manifest.outputs().getFirst();
        byte[] original=Files.readAllBytes(disk.file(output.stored().path()));Files.write(disk.file(output.stored().path()),new byte[]{1});
        assertThatThrownBy(()->ready.verify(job(),config.profiles())).hasMessageContaining("INTEGRITY");assertThat(live()).isNull();
        publication.retry(id,"tr");assertThat(publication.status(id,"tr").state()).isEqualTo("PENDING");publication.reconcile(id,"tr");assertThat(live()).isNull();assertThat(publication.status(id,"tr").state()).isEqualTo("FAILED");
        Files.write(disk.file(output.stored().path()),original);publication.retry(id,"tr");publication.reconcile(id,"tr");assertThat(live()).isNotNull();
        var proof=ready.verify(job(),config.profiles());admissions.withdraw(proof.sources().getFirst().admissionId());
        tx.executeWithoutResult(s->{guard.lock(id);assertThat(ready.guard(proof,proof.snapshot())).isFalse();});assertThat(live()).isNull();
    }
    @Test void disabledAndProductionAdmissionPreserveLegacyAndNeverPrepare() {
        config.setEnabled(false);publish();assertThat(publication.status(id,"tr").state()).isEqualTo("DISABLED");assertThat(jdbc.queryForObject("select count(*) from reading_publications",Integer.class)).isZero();
        var env=new MockEnvironment();env.setActiveProfiles("test","production");var production=new ReadingPublicationProperties(env);production.setEnabled(true);production.setProfiles(ReadingFixtures.profiles());assertThat(production.enabled()).isFalse();
    }
    @Test void sourceAdmissionPreservesOriginalRequiresExplicitBindingAndRefusesLegacyWrites() throws Exception {
        assertThat(cover).isNotEqualTo(legacy);assertThat(assets.findById(legacy)).isPresent();assertThat(assets.findById(cover).orElseThrow().getObjectPath()).contains("/reading/admitted/");
        String path=assets.findById(cover).orElseThrow().getObjectPath();var registry=new AssetStorageClientRegistry(List.of(disk));
        assertThatThrownBy(()->registry.createSignedUploadUrl(StorageProvider.FIREBASE_STORAGE,path,"image/png",Instant.now())).hasMessageContaining("RESERVED");
        assertThatThrownBy(()->registry.uploadObject(StorageProvider.FIREBASE_STORAGE,path,"image/png",1,new java.io.ByteArrayInputStream(new byte[]{1}))).hasMessageContaining("RESERVED");
        var record=admissionStore.require(cover,false);admissions.withdraw((UUID)record.get("id"));
        assertThatThrownBy(()->preparation.prepareAdmitted(assemblerSnapshot(),ReadingFixtures.profiles())).hasMessageContaining("SOURCE_NOT_ADMITTED");
    }

    @Test void olderSuccessfulObservationCannotOverwriteALaterIntegrityFailure() throws Exception {
        publish();complete();var previous=ready.verify(job(),config.profiles());
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var slow=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider provider,VersionedStorageObject object,long limit,long millis) throws java.io.IOException {
                entered.countDown();try { if(!release.await(20,TimeUnit.SECONDS)) throw new java.io.IOException("test latch timeout"); }
                catch(InterruptedException e) {Thread.currentThread().interrupt();throw new java.io.IOException(e);}
                return super.read(provider,object,limit,millis);
            }
        };
        var observer=new ReadingPublicationReadyService(preparation,admissions,jobs,jdbc,manager,slow);
        UUID current=job();
        try(var executor=Executors.newSingleThreadExecutor()) {
            var late=executor.submit(()->observer.verify(current,config.profiles()));
            assertThat(entered.await(20,TimeUnit.SECONDS)).isTrue();assertThat(live()).isEqualTo(previous.revision());jobs.integrity(previous.revision(),false,"Later withdrawal observation");release.countDown();
            assertThatThrownBy(()->late.get(30,TimeUnit.SECONDS)).hasRootCauseMessage("STALE_AVAILABILITY_OBSERVATION");
        } finally {release.countDown();}
        assertThat(live()).isNull();
    }
    @Test void admissionOrphanEvidenceSurvivesInterruptedCreateAndPinnedSourceCorruptionFailsClosed() throws Exception {
        var broken=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit,long millis) throws java.io.IOException {
                super.create(provider,path,mime,bytes,limit,millis);throw new java.io.IOException("Injected after external create");
            }
        };
        var env=new MockEnvironment();env.setActiveProfiles("test");
        var interrupted=new ReadingSourceAdmissionService(assets,broken,admissionStore,new AssetStorageFirebaseProperties(true,"synthetic","synthetic","unused","test",null,null,null),env,true);
        assertThatThrownBy(()->interrupted.admit(legacy,ReadingFixtures.profile())).hasMessageContaining("ADMISSION_VERIFICATION_FAILED");
        assertThat(jdbc.queryForObject("select count(*) from reading_source_admissions where state='PLANNED' and asset_id is null",Integer.class)).isEqualTo(1);
        publish();complete();String source=assets.findById(cover).orElseThrow().getObjectPath();Files.write(disk.file(source),ReadingFixtures.image(80,60));
        byte[] corrupt=Files.readAllBytes(disk.file(source));corrupt[corrupt.length/2]^=1;Files.write(disk.file(source),corrupt);
        assertThatThrownBy(()->ready.verify(job(),config.profiles())).hasMessageContaining("ADMITTED_SOURCE_INTEGRITY_FAILED");assertThat(live()).isNull();
    }
    @Test void languageMutationsDoNotAdvanceOrAdoptAnotherScope() {
        publish();complete();
        jdbc.update("insert into content_localizations(content_id,language_code,title,description,cover_media_id,status,processing_status) values(?,'en','English','Description',?,'DRAFT','COMPLETED')",id,cover);
        long pageId=jdbc.queryForObject("select id from story_pages where content_id=?",Long.class,id);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'en','English text',?,?)",pageId,legacy,page);
        tx.executeWithoutResult(s->publishing.publishLocalization(new PublishContentLocalizationCommand(id,LanguageCode.from("en"),Instant.now())));
        long english=publication.status(id,"en").generation();edit(cover,"Turkish edit");
        assertThat(publication.status(id,"en").generation()).isEqualTo(english);assertThat(publication.status(id,"en").liveRevision()).isNull();
        publication.reconcile(id,"en");UUID englishJob=jdbc.queryForObject("select job_id from reading_publications where content_id=? and language_code='en'",UUID.class,id);
        worker.run(englishJob);publication.reconcile(id,"en");assertThat(publication.status(id,"en").liveRevision()).isNotNull();
    }

    @Test void enabledInternalLiveDoesNotChangeOrdinaryOriginalListingOrTesterContracts() {
        publish();
        var detail=publicContents.findContent(id,TR,null);
        var originalPages=publicContents.findStoryPages(id,TR);
        var summaries=publicContents.listContentsByIds(List.of(id),TR,null);
        var listing=listingPreviews.list(List.of(id),TR,true);
        var covers=listingCovers.find(id,"tr",cover);
        var tester=experiment.findLatest(id,TR);
        assertThat(detail).isPresent();assertThat(originalPages).isPresent();
        assertThat(originalPages.orElseThrow().getFirst().illustration().assetId()).isEqualTo(page);
        assertThat(tester).isEmpty();assertThat(covers).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        complete();assertThat(live()).isNotNull();
        assertThat(publicContents.findContent(id,TR,null)).isEqualTo(detail);
        assertThat(publicContents.findStoryPages(id,TR)).isEqualTo(originalPages);
        assertThat(publicContents.listContentsByIds(List.of(id),TR,null)).isEqualTo(summaries);
        assertThat(listingPreviews.list(List.of(id),TR,true)).isEqualTo(listing);
        assertThat(listingCovers.find(id,"tr",cover)).isEqualTo(covers);
        assertThat(experiment.findLatest(id,TR)).isEqualTo(tester);
    }

    @Test void freshVerificationEnforcesParentMemoryBeforeDecodeAndOneSharedRevisionDeadline() {
        publish();publication.reconcile(id,"tr");var captured=jobs.work(job());var p=ReadingFixtures.profile();
        var huge=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),Long.MAX_VALUE/4,p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,p.growthEvidence());
        var hugeProfiles=Map.of(Role.READING_COVER,huge,Role.READING_DETAIL,huge,Role.READING_PAGE,huge);
        UUID hugeJob=cloneWork(captured,hugeProfiles);
        int priorFailures=jdbc.queryForObject("select count(*) from reading_image_integrity where not valid",Integer.class);
        assertThatThrownBy(()->ready.verify(hugeJob,hugeProfiles)).hasRootCauseMessage("READING_PARENT_HEAP_ADMISSION_EXCEEDED");
        assertThat(jdbc.queryForObject("select count(*) from reading_image_integrity where not valid",Integer.class)).isEqualTo(priorFailures);
        var shortProfile=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),50,50,1,1,true,p.growthEvidence());
        var shortProfiles=Map.of(Role.READING_COVER,shortProfile,Role.READING_DETAIL,shortProfile,Role.READING_PAGE,shortProfile);
        UUID shortJob=cloneWork(captured,shortProfiles);
        var slow=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider provider,VersionedStorageObject object,long limit,long millis) throws java.io.IOException {
                try {Thread.sleep(75);} catch(InterruptedException e) {Thread.currentThread().interrupt();throw new java.io.IOException(e);}
                return super.read(provider,object,limit,millis);
            }
        };
        var bounded=new ReadingPublicationReadyService(preparation,admissions,jobs,jdbc,manager,slow);
        assertThatThrownBy(()->bounded.verify(shortJob,shortProfiles)).satisfies(error -> {
            Throwable root=error;while(root.getCause()!=null) root=root.getCause();assertThat(root.getMessage()).isEqualTo("REVISION_DURATION_LIMIT");
        });
        assertThat(jdbc.queryForObject("select valid from reading_publication_availability where revision=?",Boolean.class,jobs.find(shortJob).revision())).isFalse();
    }
    UUID cloneWork(ReadingWork captured,Map<Role,TestProfile> profiles) {
        var descriptors=new EnumMap<Role,ReadingWork.Profile>(Role.class);profiles.forEach((role,profile)->descriptors.put(role,ReadingProfiles.capture(role,profile)));
        String revision=ReadingIdentity.hash("p3-boundary-test",UUID.randomUUID().toString());
        return jobs.schedule(new ReadingWork(captured.snapshot(),captured.sources(),descriptors,captured.sourceSet(),revision,revision)).jobId();
    }

    @Test void restartAfterExpiredDispatchLeaseRecoversIntentAndRejectsOldAttachment() {
        publish();
        var abandoned=tx.execute(s->{guard.lock(id);return store.claim(id,"tr").orElseThrow();});
        jdbc.update("update reading_publications set lease_expires_at=now()-interval '1 second' where content_id=?",id);
        publication.reconcile(id,"tr");UUID recovered=job();assertThat(recovered).isNotNull();
        tx.executeWithoutResult(s->{guard.lock(id);assertThat(store.owns(abandoned)).isFalse();store.fail(abandoned,"READING_PREPARATION_FAILED");});
        assertThat(publication.status(id,"tr").state()).isEqualTo("PREPARING");worker.run(recovered);publication.reconcile(id,"tr");assertThat(live()).isNotNull();
    }

    @Test void disablingDuringExternalVerificationCannotAdoptAnInFlightProof() {
        publish();publication.reconcile(id,"tr");UUID prepared=job();worker.run(prepared);
        var paused=mock(ReadingPublicationReadyApi.class,org.mockito.AdditionalAnswers.delegatesTo(ready));
        doAnswer(invocation->{var proof=ready.verify(prepared,config.profiles());config.setEnabled(false);return proof;}).when(paused).verify(eq(prepared),anyMap());
        new ReadingPublicationService(config,guard,contents,store,assembler,paused,manager).reconcile(id,"tr");
        assertThat(publication.status(id,"tr").state()).isEqualTo("DISABLED");
        assertThat(jdbc.queryForObject("select live_proof_json from reading_publications where content_id=?",String.class,id)).isNull();
        config.setEnabled(true);jdbc.update("update reading_publications set lease_expires_at=now()-interval '1 second' where content_id=?",id);
        publication.reconcile(id,"tr");assertThat(live()).isNotNull();
    }
    @Test void inconclusiveCapacityRejectionPreservesHealthyLiveButActualCorruptionClosesIt() throws Exception {
        publish();complete();String retained=live();UUID current=job();
        int failures=jdbc.queryForObject("select count(*) from reading_image_integrity where not valid",Integer.class);
        try(var held=ReadingPreparationCapacity.acquire()) {
            assertThatThrownBy(()->ready.verify(current,config.profiles())).hasRootCauseMessage("READING_PARENT_CAPACITY_EXHAUSTED");
            assertThat(live()).isEqualTo(retained);
        }
        assertThat(jdbc.queryForObject("select count(*) from reading_image_integrity where not valid",Integer.class)).isEqualTo(failures);
        var output=new ObjectMapper().readValue(jobs.find(current).manifest(),ReadingWork.Manifest.class).outputs().getFirst();
        Files.delete(disk.file(output.stored().path()));
        assertThatThrownBy(()->ready.verify(current,config.profiles())).hasMessageContaining("READY_INTEGRITY_FAILED");
        assertThat(live()).isNull();
    }

    @Test void newPageLocalizationCommandPersistsPositiveBindingBeforeAdvancingAndAdoptingDesiredGeneration() {
        publish();complete();String retained=live();long previous=publication.status(id,"tr").generation();
        pages.addStoryPage(new AddStoryPageCommand(id,1));
        long audio=tx.execute(s->assets.save(MediaAsset.register(StorageProvider.LOCAL_STUB,
                "/fixture/audio-"+UUID.randomUUID(),MediaKind.ORIGINAL_AUDIO)).getId());
        var result=pages.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(id,2,TR,"New localized page",audio,page));
        long persisted=jdbc.queryForObject("select l.id from story_page_localizations l join story_pages p on p.id=l.story_page_id where p.content_id=? and p.page_number=2 and l.language_code='tr'",Long.class,id);
        assertThat(persisted).isPositive();
        var captured=store.decode(jdbc.queryForObject("select snapshot_json from reading_publications where content_id=?",String.class,id),Snapshot.class);
        assertThat(captured.bindings().stream().filter(b->b.pageNumber()==2).findFirst().orElseThrow().localizationId()).isEqualTo(persisted);
        assertThat(result).isNotNull();assertThat(publication.status(id,"tr").generation()).isGreaterThan(previous);
        assertThat(live()).isEqualTo(retained);complete();assertThat(live()).isNotEqualTo(retained);
        assertThat(jobs.work(job()).snapshot()).isEqualTo(captured);
    }

    @Test void sameLimitsDifferentRuntimeIdentityRecapturesAndRetryDetachesObsoleteCompletedJobWhileRetainingLive() throws Exception {
        publish();complete();UUID current=job();var work=jobs.work(current);
        var manifest=new ObjectMapper().readValue(jobs.find(current).manifest(),ReadingWork.Manifest.class);
        var oldProfiles=new EnumMap<Role,ReadingWork.Profile>(Role.class);
        work.profiles().forEach((role,profile)->oldProfiles.put(role,new ReadingWork.Profile(profile.limits(),
                profile.descriptor()+" prior-runtime",ReadingIdentity.hash("prior-runtime",profile.fingerprint()))));
        String oldRevision=ReadingIdentity.hash("prior-runtime-revision",work.revision());
        var oldWork=new ReadingWork(work.snapshot(),work.sources(),oldProfiles,work.sourceSet(),"prior-runtime",oldRevision);
        UUID obsolete=jobs.schedule(oldWork).jobId();
        var oldOutputs=new ArrayList<ReadingWork.Output>();
        for(int i=0;i<manifest.outputs().size();i++) {
            var output=manifest.outputs().get(i);var source=work.sources().get(i);var profile=oldProfiles.get(source.binding().role());
            oldOutputs.add(new ReadingWork.Output(output.binding(),output.sourceSha256(),profile.fingerprint(),ReadingWork.rendition(source,profile),output.provider(),output.stored(),output.sha256(),output.width(),output.height()));
        }
        var oldManifest=new ReadingWork.Manifest(oldRevision,work.snapshot(),oldOutputs);
        jobs.succeed(jobs.claim(obsolete,System.currentTimeMillis()).orElseThrow(),oldManifest,System.currentTimeMillis());
        var oldProof=new ReadingPublicationReadyApi.Proof(obsolete,oldRevision,oldManifest.digest(),1,work.snapshot(),
                ready.verify(current,config.profiles()).sources(),config.profiles());
        jdbc.update("insert into reading_publication_availability(revision,valid) values(?,true)",oldRevision);
        jdbc.update("update reading_publications set live_proof_json=?,job_id=?,effective_profile_identity=? where content_id=?",
                new ObjectMapper().writeValueAsString(oldProof),obsolete,"0".repeat(64),id);
        long oldGeneration=publication.status(id,"tr").liveGeneration();
        assertThat(live()).isEqualTo(oldRevision);
        assertThatThrownBy(()->ready.verify(obsolete,config.profiles())).hasMessageContaining("CURRENT_PROFILE_CHANGED");
        assertThat(store.pending(config.profiles(),ready.effectiveProfileIdentity(config.profiles()))).hasSize(1);
        var delayed=mock(ReadingPublicationReadyApi.class,org.mockito.AdditionalAnswers.delegatesTo(ready));
        doAnswer(invocation->{var prepared=ready.prepareAdmitted(work.snapshot(),config.profiles());return new Preparation(prepared.jobId(),prepared.revision(),State.PENDING,null,null,null);}).when(delayed).prepareAdmitted(any(),anyMap());
        new ReadingPublicationService(config,guard,contents,store,assembler,delayed,manager).reconcile(id,"tr");
        var pending=publication.status(id,"tr");assertThat(pending.generation()).isGreaterThan(oldGeneration);
        assertThat(pending.liveGeneration()).isEqualTo(oldGeneration);assertThat(live()).isEqualTo(oldRevision);assertThat(job()).isNotEqualTo(obsolete);
        jdbc.update("update reading_publications set state='FAILED',job_id=? where content_id=?",obsolete,id);
        publication.retry(id,"tr");assertThat(job()).isNull();publication.reconcile(id,"tr");
        assertThat(job()).isEqualTo(current);assertThat(publication.status(id,"tr").state()).isEqualTo("LIVE");assertThat(live()).isEqualTo(work.revision());
    }
    Snapshot assemblerSnapshot() { return tx.execute(s->assembler.assemble(contents.findById(id).orElseThrow(),"tr")); }
}
