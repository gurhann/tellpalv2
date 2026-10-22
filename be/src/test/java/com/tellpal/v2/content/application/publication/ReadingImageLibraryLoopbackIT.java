package com.tellpal.v2.content.application.publication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.application.reading.ReadingSourceAdmissionService;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.api.ReadingImageConsumerApi;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.user.application.FirebaseTokenVerifier;
import com.tellpal.v2.user.application.VerifiedFirebaseToken;

/** Gerçek normal root kabulü; yalnız storage leaf ve token verifier sentetiktir. */
@EnabledIfSystemProperty(named="tellpal.package3.acceptance", matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "server.address=127.0.0.1",
    "tellpal.asset.storage.firebase.fake-client-enabled=true",
    "tellpal.asset.storage.firebase.path-prefix=test",
    "tellpal.asset.reading.test-engine-enabled=true",
    "tellpal.asset.reading.poll-delay=3600000",
    "tellpal.asset.processing.poller.enabled=false",
    "tellpal.content.reading-publication.poll-delay=3600000",
    "tellpal.security.firebase.stub-tokens-enabled=false",
    "tellpal.security.firebase.check-revoked=true",
    "tellpal.library.test.allowed-firebase-uids=synthetic-consumer",
    "tellpal.reading-consumer.enabled=true",
    "tellpal.reading-consumer.max-encoded-bytes=1000000",
    "tellpal.reading-consumer.max-pixels=500000",
    "tellpal.reading-consumer.max-memory-bytes=16000000",
    "tellpal.reading-consumer.deadline-millis=10000",
    "tellpal.reading-consumer.lease-millis=30000",
    "tellpal.reading-consumer.max-total-operations=100",
    "tellpal.reading-consumer.max-total-bytes=10000000"})
class ReadingImageLibraryLoopbackIT extends PostgresIntegrationTestBase {
    static final String RUN = "package3-"+UUID.randomUUID();
    static final Path PRIVATE_ROOT = Path.of(Objects.requireNonNullElse(System.getenv("TELLPAL_PACKAGE3_RUN"),System.getProperty("java.io.tmpdir")));
    static final Path PRIVATE = PRIVATE_ROOT.resolve(RUN);
    static final LocalDisk DISK = new LocalDisk(PRIVATE.resolve("objects"));
    @DynamicPropertySource static void runId(DynamicPropertyRegistry registry) {
        registry.add("tellpal.reading-consumer.run-id",()->RUN);
    }
    @TestBean(name="firebaseAssetStorageClient", methodName="localStorage") AssetStorageClient storage;
    static AssetStorageClient localStorage() { return DISK; }
    @MockitoBean FirebaseTokenVerifier verifier;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MediaAssetRepository assets;
    @Autowired ReadingSourceAdmissionService admissions;
    @Autowired ReadingPublicationProperties profiles;
    @Autowired ContentPublicationService publishing;
    @Autowired ReadingPublicationService publication;
    @Autowired ReadingImageWorker worker;
    @Autowired ReadingImageConsumerApi consumer;
    @LocalServerPort int port;

    @Test void realSpringNormalLibraryAppAndPageDecodeThenReleaseOnDelayedDeparture() throws Exception {
        assertThat(System.getenv("TELLPAL_READING_WEBP_TOOL_DIRECTORY")).as("Zorunlu native araç yolu").isNotBlank();
        Files.createDirectories(PRIVATE_ROOT);
        Files.createDirectory(PRIVATE);
        String credential=UUID.randomUUID().toString();
        when(verifier.verify(credential)).thenReturn(new VerifiedFirebaseToken("synthetic-consumer"));
        var profile=new TestProfile(800,600,1000000,1000000,500000,64000000,128,16,512,3000000,10000,60000,1,1,true,
                "Paket 3 sentetik yerel kabul; uretim profili degildir",FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
        profiles.setProfiles(Map.of(Role.READING_COVER,profile,Role.READING_DETAIL,profile,Role.READING_PAGE,profile));
        profiles.setEnabled(true);
        long category=jdbc.queryForObject("insert into categories(slug,type) values(?,'STORY') returning id",Long.class,RUN);
        jdbc.update("insert into category_localizations(category_id,language_code,name,status,published_at,home_display_order,home_audience) values(?,'tr','Yerel kabul','PUBLISHED',now(),0,'ALL')",category);
        var cases=new ArrayList<Map<String,Object>>();
        for(String format:List.of("JPEG","PNG")) {
            byte[] source=image(format);
            String path="test/manual/"+RUN+"/"+format;
            DISK.createImmutable(path,format.equals("JPEG")?"image/jpeg":"image/png",source);
            long legacy=new TransactionTemplate(transactions).execute(s->assets.save(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,path,MediaKind.ORIGINAL_IMAGE)).getId());
            long admitted=admissions.admit(legacy,profile).assetId();
            long id=jdbc.queryForObject("insert into contents(type,external_key,page_count,is_active,age_range) values('STORY',?,1,true,5) returning id",Long.class,RUN+format);
            jdbc.update("insert into content_localizations(content_id,language_code,title,description,cover_media_id,status,processing_status) values(?,'tr',?,'Yerel kabul açıklaması',?,'DRAFT','COMPLETED')",id,"Kabul "+format,admitted);
            long page=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,1) returning id",Long.class,id);
            jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr','Yerel sayfa',?,?)",page,legacy,admitted);
            jdbc.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,'tr',?)",category,id,cases.size());
            jdbc.update("insert into content_free_access(content_id,language_code,access_key) values(?,'tr','default')",id);
            publishing.publishLocalization(new PublishContentLocalizationCommand(id,LanguageCode.from("tr"),Instant.now()));
            publication.reconcile(id,"tr");
            UUID job=jdbc.queryForObject("select job_id from reading_publications where content_id=?",UUID.class,id);
            worker.run(job);
            assertThat(jdbc.queryForObject("select status from reading_image_jobs where id=?",String.class,job)).isEqualTo("COMPLETED");
            publication.reconcile(id,"tr");
            assertThat(publication.status(id,"tr").state()).isEqualTo("LIVE");
            var delivery=consumer.manifest(id,"tr");
            var expected=new ArrayList<Map<String,Object>>();
            for(var entry:delivery.descriptor().images()) {
                var output=entry.renditions().getFirst();
                byte[] actual=Files.readAllBytes(DISK.file(output.raster().objectKey()));
                assertThat(ReadingIdentity.sha(actual)).isEqualTo(output.raster().sha256());
                if(format.equals("JPEG")) assertThat(actual).isEqualTo(source);
                assertThat(output.raster().mime()).isEqualTo(format.equals("JPEG")?"image/jpeg":"image/webp");
                expected.add(Map.of("role",entry.source().binding().role().name(),"pageNumber",entry.source().binding().pageNumber(),"renditionKey",output.renditionKey(),"sha256",output.raster().sha256(),"byteSize",actual.length,"mime",output.raster().mime()));
            }
            cases.add(Map.of("contentId",id,"sourceFormat",format,"sourceSha256",ReadingIdentity.sha(source),"sourceBytes",source.length,"state","LIVE","outputs",expected));
        }
        var mapper=new ObjectMapper();
        Files.writeString(PRIVATE.resolve("expected.json"),mapper.writeValueAsString(Map.of("runId",RUN,"categoryId",category,"cases",cases)));
        String flutter=Objects.requireNonNull(System.getenv("TELLPAL_PACKAGE3_FLUTTER"),"Seçili kabul Flutter executable ister");
        Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        var builder=new ProcessBuilder(flutter,"test","--no-pub","--concurrency=1","--reporter=expanded","test/reading_images/reading_image_library_loopback_test.dart");
        builder.directory(root.resolve("app/mobile").toFile());
        builder.environment().put("TELLPAL_PACKAGE3_ORIGIN","http://127.0.0.1:"+port);
        builder.environment().put("TELLPAL_PACKAGE3_TOKEN",credential);
        builder.environment().put("TELLPAL_PACKAGE3_RUN",PRIVATE.toString());
        builder.redirectErrorStream(true).redirectOutput(PRIVATE.resolve("flutter-loopback.log").toFile());
        Process child=builder.start();
        var descendants=new HashSet<ProcessHandle>();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(180);
        try {
            while(child.isAlive() && System.nanoTime()<deadline) {
                child.descendants().forEach(descendants::add);
                child.waitFor(100,TimeUnit.MILLISECONDS);
            }
            assertThat(child.isAlive()).as("Flutter 180 saniye içinde biter").isFalse();
            assertThat(child.exitValue()).as("Flutter kabulü; özel flutter-loopback.log").isZero();
            var result=mapper.readTree(PRIVATE.resolve("flutter-result.json").toFile());
            assertThat(result.path("status").asText()).isEqualTo("PASS");
            assertThat(result.path("renders").size()).isEqualTo(4);
            assertThat(result.path("delayedDeparture").path("released").asBoolean()).isTrue();
            assertThat(DISK.delayStarted).isTrue();
            assertThat(DISK.delayFinished.await(5,TimeUnit.SECONDS)).as("Sunucu gecikmesi gerçekten tamamlanır").isTrue();
            assertThat(DISK.delayOutcome).isEqualTo("RELEASED");
            assertThat(DISK.delayTimedOut).isFalse();
            assertThat(DISK.clientCleanupObserved).isTrue();
        } finally {
            cleanup(child,descendants);
        }
    }
    private void cleanup(Process child,Set<ProcessHandle> descendants) {
        var errors=new ArrayList<Throwable>();
        boolean interrupted=Thread.interrupted();
        try {
            cleanupStep(errors,()->{
                Path release=PRIVATE.resolve("delay-release");
                if(!Files.exists(release)) Files.writeString(release,RUN,StandardOpenOption.CREATE_NEW);
            });
        } finally {
            cleanupStep(errors,()->child.descendants().forEach(descendants::add));
            for(var process:descendants) cleanupStep(errors,()->{if(process.isAlive())process.destroyForcibly();});
            cleanupStep(errors,()->{if(child.isAlive())child.destroyForcibly();});
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            cleanupStep(errors,()->child.waitFor(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS));
            for(var process:descendants) cleanupStep(errors,()->{
                if(process.isAlive()) process.onExit().get(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
            });
            cleanupStep(errors,()->assertThat(child.isAlive()).as("Flutter child çıktı").isFalse());
            for(var process:descendants) cleanupStep(errors,()->assertThat(process.isAlive()).as("Gözlenen descendant çıktı").isFalse());
            cleanupStep(errors,()->profiles.setEnabled(false));
            if(interrupted || errors.stream().anyMatch(e->e instanceof InterruptedException)) Thread.currentThread().interrupt();
        }
        if(!errors.isEmpty()) {
            var failure=new AssertionError("Paket 3 zorunlu cleanup tamamlanamadı");
            errors.forEach(failure::addSuppressed);
            throw failure;
        }
    }
    @FunctionalInterface private interface CleanupStep { void run() throws Exception; }
    private static void cleanupStep(List<Throwable> errors,CleanupStep step) {
        try { step.run(); }
        catch(Exception | AssertionError error) { errors.add(error); }
    }
    static byte[] image(String format) throws IOException {
        var image=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<600;y++) for(int x=0;x<800;x++) image.setRGB(x,y,y<300?(x<400?0xff0000:0x00ff00):(x<400?0x0000ff:0xffff00));
        var bytes=new ByteArrayOutputStream();
        assertThat(ImageIO.write(image,format,bytes)).isTrue();
        return bytes.toByteArray();
    }
    static final class LocalDisk extends ReadingFixtures.DiskStorage {
        volatile boolean delayStarted,delayTimedOut,clientCleanupObserved;
        volatile String delayOutcome="NOT_STARTED";
        final CountDownLatch delayFinished=new CountDownLatch(1);
        LocalDisk(Path directory) { super(directory); }
        @Override public InputStream openVersion(VersionedStorageObject object) {
            Path arm=PRIVATE.resolve("delay-arm");
            if(object.path().contains("/reading/renditions/") && Files.exists(arm)) {
                try {
                    Files.delete(arm);
                    delayStarted=true;
                    delayOutcome="WAITING";
                    Files.writeString(PRIVATE.resolve("delay-waiting"),RUN,StandardOpenOption.CREATE_NEW);
                    Files.writeString(PRIVATE.resolve("delay-started"),RUN,StandardOpenOption.CREATE_NEW);
                    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
                    while(true) {
                        if(System.nanoTime()>=deadline) {
                            delayTimedOut=true;
                            delayOutcome="TIMED_OUT";
                            throw new IllegalStateException("DELAY_RELEASE_REQUIRED");
                        }
                        if(Files.exists(PRIVATE.resolve("delay-release"))) {
                            assertThat(Files.readString(PRIVATE.resolve("delay-release"))).isEqualTo(RUN);
                            var cleanup=new ObjectMapper().readTree(PRIVATE.resolve("client-cleanup.json").toFile());
                            assertThat(cleanup.path("runId").asText()).isEqualTo(RUN);
                            assertThat(cleanup.path("activeOwners").asInt(-1)).isZero();
                            assertThat(cleanup.path("closed").asBoolean()).isTrue();
                            assertThat(cleanup.path("serverWaitingObserved").asBoolean()).isTrue();
                            clientCleanupObserved=true;
                            delayOutcome="RELEASED";
                            break;
                        }
                        Thread.sleep(10);
                    }
                } catch(IOException e) { delayOutcome="FAILED";throw new UncheckedIOException(e); }
                catch(InterruptedException e) { delayOutcome="INTERRUPTED";Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                finally {
                    if(delayOutcome.equals("WAITING")) delayOutcome="FAILED";
                    try {
                        Files.deleteIfExists(PRIVATE.resolve("delay-waiting"));
                        Files.writeString(PRIVATE.resolve("delay-finished.json"),new ObjectMapper().writeValueAsString(Map.of(
                                "runId",RUN,"outcome",delayOutcome,"timedOut",delayTimedOut,"clientCleanupObserved",clientCleanupObserved)),StandardOpenOption.CREATE_NEW);
                    } catch(IOException e) { throw new UncheckedIOException(e); }
                    finally { delayFinished.countDown(); }
                }
            }
            return super.openVersion(object);
        }
    }
}
