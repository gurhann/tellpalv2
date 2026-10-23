package com.tellpal.v2.content.application.publication;

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
import com.tellpal.v2.content.api.*;
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
class ReadingImageComparisonServiceIT extends PostgresIntegrationTestBase {
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
        publication=new ReadingPublicationService(config,guard,contents,store,assembler,ready,admissions,manager);
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
    ReadingPublicationReadyApi.Proof proof() {return store.decode(jdbc.queryForObject("select live_proof_json from reading_publications where content_id=?",String.class,id),ReadingPublicationReadyApi.Proof.class);}
    ReadingImageDeliveryApi.Selection selection(ReadingPublicationReadyApi.Proof proof) {
        var descriptor=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)).descriptor(proof);
        return new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,descriptor.images().getFirst().renditions().getFirst().renditionKey(),false);
    }
    ReadingImageDeliveryApi.Limits runLimits(String run,long operations,long bytes) {return new ReadingImageDeliveryApi.Limits(1000000,100000,10000000,10000,run,operations,bytes);}
    @Test void transport429503AndResetPreserveLiveButAuthoritativeMissingWithdraws() throws Exception {
        publish();complete();var proof=proof();var selection=selection(proof);
        for(Throwable failure:List.of(new com.google.cloud.storage.StorageException(429,"quota"),new com.google.cloud.storage.StorageException(503,"unavailable"),new java.net.SocketException("reset"))) {
            var transport=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
                @Override public byte[] read(StorageProvider p,VersionedStorageObject object,long limit,long millis)throws java.io.IOException {
                    if(failure instanceof java.io.IOException io)throw io;throw (RuntimeException)failure;
                }
            };
            assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,transport,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,runLimits("trans-"+UUID.randomUUID(),1,1000000)))
                    .hasMessageContaining("TRANSPORT");assertThat(live()).isEqualTo(proof.revision());
        }
        var missing=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider p,VersionedStorageObject object,long limit,long millis){throw new com.google.cloud.storage.StorageException(404,"missing pinned generation");}
        };
        assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,missing,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,runLimits("missing-"+UUID.randomUUID(),1,1000000))).hasMessageContaining("INTEGRITY");
        assertThat(live()).isNull();
    }
    @Test void completePinnedWrongShaWithdrawsLive() throws Exception {assertCompleteIntegrity(false);}
    @Test void completePinnedWrongMimeWithdrawsLive() throws Exception {assertCompleteIntegrity(true);}
    void assertCompleteIntegrity(boolean wrongMime) throws Exception {
        publish();complete();var proof=proof();var selection=selection(proof);
        if(wrongMime) {
            var jpeg=new java.io.ByteArrayOutputStream();var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(ReadingFixtures.image(40,30)));javax.imageio.ImageIO.write(image,"jpeg",jpeg);image.flush();
            var storage=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
                @Override public byte[] read(StorageProvider p,VersionedStorageObject object,long limit,long millis){return jpeg.toByteArray();}
            };
            assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,storage,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,runLimits("mime-"+UUID.randomUUID(),1,1000000))).hasMessageContaining("INTEGRITY");
        } else {
            var manifest=new ObjectMapper().readValue(jobs.find(proof.jobId()).manifest(),ReadingWork.Manifest.class);var first=manifest.outputs().getFirst();var outputs=new ArrayList<>(manifest.outputs());
            outputs.set(0,new ReadingWork.Output(first.binding(),first.sourceSha256(),first.profileFingerprint(),first.renditionKey(),first.provider(),first.stored(),"0".repeat(64),first.width(),first.height()));
            var wrong=new ReadingWork.Manifest(manifest.revision(),manifest.snapshot(),outputs);var saved=jobs.find(proof.jobId());
            var altered=mock(ReadingJobStore.class,org.mockito.AdditionalAnswers.delegatesTo(jobs));
            doReturn(new Preparation(saved.jobId(),saved.revision(),saved.state(),wrong.digest(),wrong.canonical(),null)).when(altered).find(proof.jobId());
            var expected=new ReadingPublicationReadyApi.Proof(proof.jobId(),proof.revision(),wrong.digest(),proof.availabilityEpoch(),proof.snapshot(),proof.sources(),proof.profiles());
            assertThatThrownBy(()->new ReadingImageDeliveryService(altered,disk.storage(),new ReadingComparisonBudget(jdbc,manager)).read(expected,selection,runLimits("sha-"+UUID.randomUUID(),1,1000000))).hasMessageContaining("INTEGRITY");
        }
        assertThat(live()).isNull();
    }
    @Test void successfulAndFailedPinnedDeliverySpendBudgetBeforeStorageAndAcrossServiceRestart() throws Exception {
        publish();complete();var proof=proof();var selection=selection(proof);var reads=new java.util.concurrent.atomic.AtomicInteger();
        var counted=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider p,VersionedStorageObject object,long limit,long millis)throws java.io.IOException {reads.incrementAndGet();return super.read(p,object,limit,millis);}
        };
        var limits=runLimits("read-"+UUID.randomUUID(),1,1000000);
        byte[] bytes=new ReadingImageDeliveryService(jobs,counted,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,limits);
        assertThat(jdbc.queryForObject("select reserved_bytes from reading_comparison_budgets where run_id=?",Long.class,limits.runId())).isEqualTo(bytes.length);
        assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,counted,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,limits)).hasMessageContaining("RESOURCE");assertThat(reads.get()).isEqualTo(1);
        var failedReads=new java.util.concurrent.atomic.AtomicInteger();
        var unavailable=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider p,VersionedStorageObject object,long limit,long millis){failedReads.incrementAndGet();throw new com.google.cloud.storage.StorageException(503,"temporary");}
        };
        var failedLimits=runLimits("failed-"+UUID.randomUUID(),1,1000000);
        assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,unavailable,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,failedLimits)).hasMessageContaining("TRANSPORT");
        assertThat(jdbc.queryForObject("select operations from reading_comparison_budgets where run_id=?",Long.class,failedLimits.runId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select reserved_bytes from reading_comparison_budgets where run_id=?",Long.class,failedLimits.runId())).isEqualTo(bytes.length);
        assertThatThrownBy(()->new ReadingImageDeliveryService(jobs,unavailable,new ReadingComparisonBudget(jdbc,manager)).read(proof,selection,failedLimits)).hasMessageContaining("RESOURCE");assertThat(failedReads.get()).isEqualTo(1);assertThat(live()).isNotNull();
    }
    @Test void postIoRevokedBearerReturns401WithoutImageOrWithdrawalThroughActualServiceController() throws Exception {assertPostIoAuth(true);}
    @Test void freeChangeDuringSuccessfulSecondBearerRequiresFinalDbCut() throws Exception {assertPostIoAuth(false);}
    void assertPostIoAuth(boolean revoked) throws Exception {
        publish();complete();freeCopy();var api=comparison(new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)));
        var admitted=api.manifest(id,"tr");var identity=admitted.identity();var tokens=mock(com.tellpal.v2.user.application.FirebaseTokenVerifier.class);
        var verifications=new java.util.concurrent.atomic.AtomicInteger();
        when(tokens.verify("ephemeral")).thenAnswer(call->{
            if(verifications.incrementAndGet()==2){
                if(revoked)throw new com.tellpal.v2.user.application.UserApplicationExceptions.FirebaseTokenVerificationException("revoked");
                jdbc.update("delete from content_free_access where content_id=?",id);
            }
            return new com.tellpal.v2.user.application.VerifiedFirebaseToken("jLRypTmoBrhhX21HsDJojurcKTC3");
        });
        var actual=new com.tellpal.v2.user.application.ReadingImageComparisonTestService(tokens,new com.tellpal.v2.user.infrastructure.firebase.FirebaseAuthenticationProperties(false,"synthetic",null,true),api);
        var controller=new com.tellpal.v2.user.web.mobile.ReadingImageComparisonTestController(actual,new com.tellpal.v2.user.web.mobile.ReadingImageComparisonResponseWriter(10000));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).addFilters(new com.tellpal.v2.user.web.mobile.ReadingImageComparisonCacheFilter()).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/test/reading-image-comparison/bytes")
                .requestAttr("com.tellpal.v2.user.web.mobile.ReadingImageComparisonTomcatConfiguration.abort",(Runnable)()->{})
                .with(request->{request.setAsyncSupported(true);return request;})
                .header("Authorization","Bearer ephemeral").param("contentId",""+id).param("language","tr").param("liveGeneration",""+identity.liveGeneration())
                .param("revision",identity.revision()).param("manifestDigest",identity.manifestDigest()).param("jobId",identity.jobId().toString())
                .param("role","READING_COVER").param("pageNumber","0").param("renditionKey",admitted.descriptor().images().getFirst().renditions().getFirst().renditionKey()).param("canonicalSource","false"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(revoked?401:403))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control","private,no-store"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("application/problem+json"));
        verify(tokens,times(2)).verify("ephemeral");assertThat(live()).isNotNull();
    }
    ReadingImageComparisonService comparison(ReadingImageDeliveryApi delivery) {
        var env=new MockEnvironment(); env.setActiveProfiles("test");
        var limits=new ReadingImageComparisonProperties(env,true,id,"tr","synthetic",1000000,100000,10000000,10000,3000,100,10000000);
        return new ReadingImageComparisonService(limits,guard,contents,free,store,ready,delivery,manager);
    }
    @Autowired ContentFreeAccessApi free;
    void freeCopy() {
        long pageId=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,2) returning id",Long.class,id);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr','Second',?,?)",pageId,legacy,page);
        jdbc.update("update contents set page_count=2 where id=?",id);
        edit(cover,"Two pages");complete();
 jdbc.update("insert into content_free_access(content_id,language_code,access_key) values(?,'tr','default')",id); }
    @Test void retainedJobComesFromLiveProofAndPinnedBytesAreVerified() throws Exception {
        publish();complete();freeCopy();
        var api=comparison(new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)));
        var admitted=api.manifest(id,"tr"); UUID retained=admitted.identity().jobId();
        edit(legacy,"Failed replacement");publication.reconcile(id,"tr");
        assertThat(api.manifest(id,"tr").identity().jobId()).isEqualTo(retained);
        var image=admitted.descriptor().images().getFirst();
        var selection=new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,image.renditions().getFirst().renditionKey(),false);
        assertThat(api.read(admitted.identity(),selection)).hasSize((int)image.renditions().getFirst().raster().byteSize());
        assertThatThrownBy(()->api.manifest(id,"en")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void freeAccessChangeDuringIoRejectsFinalResponseAndDoesNotWithdrawHealthyMedia() {
        publish();complete();freeCopy();
        var real=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager));
        var changed=mock(ReadingImageDeliveryApi.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        doAnswer(call->{byte[] bytes=real.read(call.getArgument(0),call.getArgument(1),call.getArgument(2));
            jdbc.update("delete from content_free_access where content_id=?",id);return bytes;}).when(changed).read(any(),any(),any());
        var api=comparison(changed);var admitted=api.manifest(id,"tr");
        var key=admitted.descriptor().images().getFirst().renditions().getFirst().renditionKey();
        assertThatThrownBy(()->api.read(admitted.identity(),new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,key,false)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(live()).isNotNull();
    }
    @Test void corruptPinnedOutputWithdrawsAvailabilityAndParallelCapacityRefusesWithoutWithdrawal() throws Exception {
        publish();complete();freeCopy();var api=comparison(new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)));
        var admitted=api.manifest(id,"tr");var output=admitted.descriptor().images().getFirst().renditions().getFirst();
        var selection=new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,output.renditionKey(),false);
        try(var capacity=ReadingPreparationCapacity.acquire()) {
            assertThatThrownBy(()->api.read(admitted.identity(),selection)).hasMessageContaining("RESOURCE");
        }
        assertThat(live()).isNotNull();
        Files.write(disk.file(output.raster().objectKey()),new byte[]{1});
        assertThatThrownBy(()->api.read(admitted.identity(),selection)).hasMessageContaining("INTEGRITY");
        assertThatThrownBy(()->api.status(admitted.identity())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void durableRunReservationsDoNotResetOrRaiseCeilings() {
        var budget=new ReadingComparisonBudget(jdbc,manager);String run="run-"+UUID.randomUUID();
        var limits=new ReadingImageDeliveryApi.Limits(100,100,2000,1000,run,1,100);
        budget.reserve(limits,100);
        assertThatThrownBy(()->new ReadingComparisonBudget(jdbc,manager).reserve(limits,1)).hasMessageContaining("TOTAL_RESOURCE_LIMIT");
        assertThatThrownBy(()->budget.reserve(new ReadingImageDeliveryApi.Limits(100,100,2000,1000,run,2,200),1)).hasMessageContaining("TOTAL_RESOURCE_LIMIT");
        assertThat(jdbc.queryForObject("select reserved_bytes from reading_comparison_budgets where run_id=?",Long.class,run)).isEqualTo(100);
    }
    @Test void lockedCopyAndForgedGenerationRefuseBeforeStorage() {
        publish();complete();var delivery=mock(ReadingImageDeliveryApi.class);var api=comparison(delivery);
        assertThatThrownBy(()->api.manifest(id,"tr")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(delivery);freeCopy();
        var real=comparison(new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)));var identity=real.manifest(id,"tr").identity();
        assertThatThrownBy(()->real.read(identity,new ReadingImageDeliveryApi.Selection(Role.READING_PAGE,0,"wrong-role",true)))
                .isInstanceOf(IllegalArgumentException.class);
        var stale=new ReadingImageComparisonApi.Identity(id,"tr",identity.liveGeneration()+1,identity.revision(),identity.manifestDigest(),identity.jobId());
        assertThatThrownBy(()->api.read(stale,new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,"invalid",true)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(delivery);
    }

    @Test void consumerDetailAndOrderedPagesShareOneRetainedManifestWithoutOpeningPublicSurfaces() {
        publish();complete();freeCopy();
        jdbc.update("update content_localizations set processing_status='PENDING' where content_id=? and language_code='tr'",id);
        var api=comparison(new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager)));
        var admitted=api.manifest(id,"tr");
        var entries=admitted.descriptor().images().stream()
                .filter(e->e.source().binding().role()!=Role.READING_COVER).toList();
        assertThat(entries).extracting(e->e.source().binding().role())
                .containsExactly(Role.READING_DETAIL,Role.READING_PAGE,Role.READING_PAGE);
        assertThat(entries).extracting(e->e.source().binding().pageNumber()).containsExactly(0,1,2);
        assertThat(entries.subList(1,3)).extracting(e->e.source().binding().pageId())
                .containsExactlyElementsOf(jdbc.queryForList("select id from story_pages where content_id=? order by page_number",Long.class,id));
        for(var entry:entries) {
            var binding=entry.source().binding();var output=entry.renditions().getFirst();
            byte[] bytes=api.read(admitted.identity(),new ReadingImageDeliveryApi.Selection(binding.role(),binding.pageNumber(),output.renditionKey(),false));
            assertThat(bytes).hasSize((int)output.raster().byteSize());
            assertThat(java.util.HexFormat.of().formatHex(sha256(bytes))).isEqualTo(output.raster().sha256());
            assertThat(api.status(admitted.identity()).identity()).isEqualTo(admitted.identity());
        }
        assertThat(publicContents.listContentsByIds(List.of(id),TR,null)).isEmpty();
        assertThat(publicContents.findContent(id,TR,null)).isEmpty();
        assertThat(publicContents.findStoryPages(id,TR)).isEmpty();
        assertThat(listingPreviews.list(List.of(id),TR,false)).isEmpty();
        assertThat(listingCovers.find(id,"tr",cover)).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        assertThat(experiment.findLatest(id,TR)).isEmpty();
        assertThat(jdbc.queryForObject("select processing_status from content_localizations where content_id=? and language_code='tr'",String.class,id)).isEqualTo("PENDING");
    }
    byte[] sha256(byte[] bytes) {
        try {return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);}
        catch(java.security.NoSuchAlgorithmException impossible) {throw new AssertionError(impossible);}
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans={false,true})
    void replacedLiveRefusesOldConsumerAnchorBeforeAndAfterStorage(boolean duringIo) throws Exception {
        publish();complete();freeCopy();
        long replacement=admissions.admit(original("replacement",82),ReadingFixtures.profile()).assetId();
        var real=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager));
        var delivery=mock(ReadingImageDeliveryApi.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        var api=comparison(delivery);var admitted=api.manifest(id,"tr");
        var detail=admitted.descriptor().images().stream().filter(e->e.source().binding().role()==Role.READING_DETAIL).findFirst().orElseThrow();
        var selection=new ReadingImageDeliveryApi.Selection(Role.READING_DETAIL,0,detail.renditions().getFirst().renditionKey(),false);
        clearInvocations(delivery);
        if(duringIo) {
            doAnswer(call->{byte[] bytes=real.read(call.getArgument(0),call.getArgument(1),call.getArgument(2));
                edit(replacement,"New LIVE");complete();return bytes;}).when(delivery).read(any(),any(),any());
        } else {edit(replacement,"New LIVE");complete();}
        assertThatThrownBy(()->api.read(admitted.identity(),selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(delivery,times(duringIo?1:0)).read(any(),any(),any());
        assertThat(api.manifest(id,"tr").identity()).isNotEqualTo(admitted.identity());
        assertThat(live()).isNotNull();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"archive-before","archive-after","inactive-before","inactive-after","free-before","free-after"})
    void closedEditorialAccessRefusesConsumerBytesAtBothDbCuts(String scenario) {
        publish();complete();freeCopy();
        var real=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager));
        var delivery=mock(ReadingImageDeliveryApi.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        var api=comparison(delivery);var admitted=api.manifest(id,"tr");
        var detail=admitted.descriptor().images().stream().filter(e->e.source().binding().role()==Role.READING_DETAIL).findFirst().orElseThrow();
        var selection=new ReadingImageDeliveryApi.Selection(Role.READING_DETAIL,0,detail.renditions().getFirst().renditionKey(),false);
        Runnable close=()->{
            if(scenario.startsWith("archive")) tx.executeWithoutResult(s->publishing.archiveLocalization(new ArchiveContentLocalizationCommand(id,TR)));
            else if(scenario.startsWith("inactive")) management.updateContent(new UpdateContentCommand(id,external,null,false));
            else jdbc.update("delete from content_free_access where content_id=?",id);
        };
        boolean duringIo=scenario.endsWith("after");clearInvocations(delivery);
        if(duringIo) doAnswer(call->{byte[] bytes=real.read(call.getArgument(0),call.getArgument(1),call.getArgument(2));close.run();return bytes;})
                .when(delivery).read(any(),any(),any());
        else close.run();
        assertThatThrownBy(()->api.read(admitted.identity(),selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(delivery,times(duringIo?1:0)).read(any(),any(),any());
        assertThatThrownBy(()->api.status(admitted.identity())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
