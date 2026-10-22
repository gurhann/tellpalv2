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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "tellpal.content.reading-publication.poll-delay=3600000",
    "tellpal.security.firebase.stub-tokens-enabled=false","tellpal.security.firebase.check-revoked=true",
    "tellpal.library.test.allowed-firebase-uids=synthetic-consumer",
    "tellpal.reading-consumer.deadline-millis=10000"})
class ReadingImageConsumerIT extends PostgresIntegrationTestBase {
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
    ReadingPublicationReadyApi.Proof proof() {return store.decode(jdbc.queryForObject("select live_proof_json from reading_publications where content_id=?",String.class,id),ReadingPublicationReadyApi.Proof.class);}

    @Autowired ContentFreeAccessApi free;
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @org.springframework.test.context.bean.override.mockito.MockitoBean ReadingImageConsumerApi httpApi;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.tellpal.v2.user.application.FirebaseTokenVerifier tokens;
    ReadingImageConsumerApi api(ReadingImageDeliveryApi delivery) {
        var env=new MockEnvironment();env.setActiveProfiles("test");
        var limits=new ReadingImageConsumerProperties(env,true,"consumer-"+UUID.randomUUID(),1000000,100000,10000000,10000,3000,1000,10000000);
        return new ReadingImageConsumerService(limits,guard,new ReadingImageRetainedAccess(guard,contents,free,store,ready),delivery,manager);
    }
    void threePages() {
        for(int number=2;number<=3;number++) {
            long pageId=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,?) returning id",Long.class,id,number);
            jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr','Ek sayfa',?,?)",pageId,legacy,page);
        }
        jdbc.update("update contents set page_count=3 where id=?",id);
        jdbc.update("insert into content_free_access(content_id,language_code,access_key) values(?,'tr','default')",id);
    }
    TestProfile preservation() {
        var p=ReadingFixtures.profile();
        return new TestProfile(100,100,p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,"Yerel tuketici fixture growth kaniti",FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"PNG","JPEG"})
    void realHttpDeliversOnlyVerifiedJpegOrWebpForThreePagesAndRejectsClosedAccess(String format) throws Exception {
        com.tellpal.v2.asset.reading.ReadingNativeTestTools.require();
        var p=preservation();config.setProfiles(Map.of(Role.READING_COVER,p,Role.READING_DETAIL,p,Role.READING_PAGE,p));
        String path="test/manual/preserve-"+UUID.randomUUID();
        byte[] source=ReadingFixtures.quadrants(format);
        disk.createImmutable(path,format.equals("JPEG")?"image/jpeg":"image/png",source);
        long legacyId=tx.execute(s->assets.save(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,path,MediaKind.ORIGINAL_IMAGE)).getId());
        long admittedId=admissions.admit(legacyId,p).assetId();
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?",admittedId,id);
        jdbc.update("update story_page_localizations set illustration_media_id=? where story_page_id in(select id from story_pages where content_id=?)",admittedId,id);
        page=admittedId;threePages();publish();complete();
        var real=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager));
        var api=api(real);
        doAnswer(c->{api.requireConfigured(c.getArgument(0),c.getArgument(1));return null;}).when(httpApi).requireConfigured(anyLong(),anyString());
        when(httpApi.manifest(anyLong(),anyString())).thenAnswer(c->api.manifest(c.getArgument(0),c.getArgument(1)));
        when(httpApi.status(any())).thenAnswer(c->api.status(c.getArgument(0)));
        when(httpApi.read(any(),any())).thenAnswer(c->api.read(c.getArgument(0),c.getArgument(1)));
        when(tokens.verify("ephemeral")).thenReturn(new com.tellpal.v2.user.application.VerifiedFirebaseToken("synthetic-consumer"));
        var client=java.net.http.HttpClient.newHttpClient();
        var manifest=request(client,"manifest?contentId="+id+"&language=tr");
        assertThat(manifest.statusCode()).isEqualTo(200);assertThat(manifest.headers().firstValue("Cache-Control")).contains("private,no-store");
        var tree=new ObjectMapper().readTree(manifest.body());assertThat(tree.path("accountScope").asText()).isEqualTo("synthetic-consumer");
        var admission=api.manifest(id,"tr");assertThat(admission.descriptor().images()).hasSize(5);
        for(var entry:admission.descriptor().images()) {
            var binding=entry.source().binding();var output=entry.renditions().getFirst();
            var expected=new ReadingImageDeliveryApi.Selection(binding.role(),binding.pageNumber(),output.renditionKey(),false);
            var payload=api.read(admission.identity(),expected);
            String query=query(admission.identity())+"&role="+binding.role()+"&pageNumber="+binding.pageNumber()+"&renditionKey="+output.renditionKey();
            var response=request(client,"bytes?"+query);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type")).contains(format.equals("JPEG")?"image/jpeg":"image/webp");
            assertThat(response.body()).isEqualTo(payload.bytes());
            assertThat(ReadingIdentity.sha(response.body())).isEqualTo(output.raster().sha256());
            if(format.equals("JPEG"))assertThat(response.body()).isEqualTo(source);
        }
        var output=admission.descriptor().images().getFirst().renditions().getFirst();
        assertThatThrownBy(()->api.read(admission.identity(),new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,output.renditionKey(),true))).hasMessage("SOURCE_DELIVERY_FORBIDDEN");
        assertThat(request(client,"status?"+query(admission.identity()).replace("synthetic-consumer","wrong-account")).statusCode()).isEqualTo(403);
        assertThat(request(client,"status?"+query(admission.identity()).replace("language=tr","language=en")).statusCode()).isEqualTo(403);
        jdbc.update("delete from content_free_access where content_id=?",id);
        assertThat(request(client,"status?"+query(admission.identity())).statusCode()).isEqualTo(403);
        assertThat(request(client,"bytes?"+query(admission.identity())+"&role=READING_COVER&pageNumber=0&renditionKey="+output.renditionKey()).statusCode()).isEqualTo(403);
        assertThat(live()).isNotNull();
    }
    @Test void ordinaryBytesRecheckFreeAfterIoAndNeverAdmitStaleIdentityOrCorruption() throws Exception {
        threePages();publish();complete();
        var real=new ReadingImageDeliveryService(jobs,disk.storage(),new ReadingComparisonBudget(jdbc,manager));
        var changed=mock(ReadingImageDeliveryApi.class,org.mockito.AdditionalAnswers.delegatesTo(real));
        var api=api(changed);var admission=api.manifest(id,"tr");
        var output=admission.descriptor().images().getFirst().renditions().getFirst();
        var selection=new ReadingImageDeliveryApi.Selection(Role.READING_COVER,0,output.renditionKey(),false);
        var i=admission.identity();
        var stale=new ReadingImageComparisonApi.Identity(id,"tr",i.liveGeneration()+1,i.revision(),i.manifestDigest(),i.jobId());
        assertThatThrownBy(()->api.read(stale,selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verify(changed,never()).readRendition(any(),any(),any());
        doAnswer(call->{var bytes=real.readRendition(call.getArgument(0),call.getArgument(1),call.getArgument(2));
            jdbc.update("delete from content_free_access where content_id=?",id);return bytes;}).when(changed).readRendition(any(),any(),any());
        assertThatThrownBy(()->api.read(i,selection)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(live()).isNotNull();
        jdbc.update("insert into content_free_access(content_id,language_code,access_key) values(?,'tr','default')",id);
        var ordinary=api(real);
        try(var capacity=ReadingPreparationCapacity.acquire()) {assertThatThrownBy(()->ordinary.read(i,selection)).hasMessageContaining("RESOURCE");}
        assertThat(live()).isNotNull();
        Files.write(disk.file(output.raster().objectKey()),new byte[]{1,2,3});
        assertThatThrownBy(()->ordinary.read(i,selection)).hasMessageContaining("INTEGRITY");
        assertThatThrownBy(()->ordinary.status(i)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    String query(ReadingImageComparisonApi.Identity i) {
        return "accountScope=synthetic-consumer&contentId="+i.contentId()+"&language="+i.language()+"&liveGeneration="+i.liveGeneration()+"&revision="+i.revision()+"&manifestDigest="+i.manifestDigest()+"&jobId="+i.jobId();
    }
    java.net.http.HttpResponse<byte[]> request(java.net.http.HttpClient client,String path) throws Exception {
        return client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/library/reading-images/"+path)).header("Authorization","Bearer ephemeral").GET().build(),java.net.http.HttpResponse.BodyHandlers.ofByteArray());
    }
}
