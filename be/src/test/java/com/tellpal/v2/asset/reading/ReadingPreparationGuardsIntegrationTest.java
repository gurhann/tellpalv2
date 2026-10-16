package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.content.api.ReadingImageSourceSnapshotApi;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
class ReadingPreparationGuardsIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired ReadingJobStore jobs;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ReadingImageSourceSnapshotApi snapshots;
    @MockitoSpyBean ContentRepository contents;
    @TempDir Path directory;
    ReadingFixtures.DiskStorage disk;
    MediaAssetRepository assets;
    ReadingImagePreparationService service;

    @BeforeEach void setup() throws Exception {
        jdbc.execute("truncate reading_image_integrity,reading_image_attempt_evidence,reading_image_manifests,reading_image_jobs cascade");
        disk=new ReadingFixtures.DiskStorage(directory);
        disk.createImmutable("original/cover","image/png",ReadingFixtures.image(80,60));
        disk.createImmutable("original/page","image/png",ReadingFixtures.image(80,60));
        assets=mock(MediaAssetRepository.class);
        when(assets.findById(3L)).thenReturn(Optional.of(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,"original/cover",MediaKind.ORIGINAL_IMAGE)));
        when(assets.findById(4L)).thenReturn(Optional.of(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,"original/page",MediaKind.ORIGINAL_IMAGE)));
        service=service(disk.storage());
    }
    private ReadingImagePreparationService service(ReadingObjectStorage storage) {
        var environment=new MockEnvironment();environment.setActiveProfiles("test");
        return new ReadingImagePreparationService(assets,storage,jobs,environment,
                new AssetStorageFirebaseProperties(true,"fixture","fixture","unused","test",null,null,null),true, org.mockito.Mockito.mock(com.tellpal.v2.asset.application.reading.ReadingSourceAdmissionService.class));
    }
    private TestProfile budget(long millis) {
        var p=ReadingFixtures.profile();
        return new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),millis,millis,1,1,true,"synthetic bounded guard fixture");
    }
    private Map<Role,TestProfile> profiles(TestProfile profile) {
        return Map.of(Role.READING_COVER,profile,Role.READING_DETAIL,profile,Role.READING_PAGE,profile);
    }
    private ReadingImageWorker fastWorker(ReadingObjectStorage storage) {
        return new ReadingImageWorker(service,jobs,storage,Clock.systemUTC(),new ReadingImageProcess() {
            @Override public byte[] convert(byte[] original,TestProfile profile) throws IOException { return ReadingFixtures.image(40,30); }
        });
    }
    private Pending pendingWithVerifiedManifest() throws Exception {
        var prepared=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC()).run(prepared.jobId());
        var ready=service.find(prepared.jobId());assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);var work=jobs.work(prepared.jobId());
        jdbc.execute("truncate reading_image_integrity,reading_image_attempt_evidence,reading_image_manifests,reading_image_jobs cascade");
        return new Pending(jobs.schedule(work),manifest);
    }
    private record Pending(Preparation job,Manifest manifest) { }
    private Attempt token(UUID id) {
        var row=jdbc.queryForMap("select * from reading_image_jobs where id=?",id);
        return new Attempt(id,((Number)row.get("attempt")).intValue(),(UUID)row.get("lease"),((java.sql.Timestamp)row.get("lease_expires_at")).getTime(),
                (String)row.get("source_set"),(String)row.get("profile_set"));
    }
    @Test void leaseExpiryDuringRealPostgresRowLockWaitRejectsBothSuccessAndFailure() throws Exception {
        var pending=pendingWithVerifiedManifest();UUID id=pending.job().jobId();
        var initial=jobs.claim(id,System.currentTimeMillis()).orElseThrow();
        for(boolean success:List.of(true,false)) {
            long expiry=System.currentTimeMillis()+500;
            jdbc.update("update reading_image_jobs set lease_expires_at=? where id=?",new java.sql.Timestamp(expiry),id);
            Attempt token=token(id);long callerNow=System.currentTimeMillis();
            assertThat(callerNow).isLessThan(token.expiresEpochMillis());
            try(var locker=jdbc.getDataSource().getConnection();var executor=Executors.newSingleThreadExecutor()) {
                locker.setAutoCommit(false);
                try(var statement=locker.prepareStatement("select id from reading_image_jobs where id=? for update")) {
                    statement.setObject(1,id);statement.executeQuery().close();
                }
                var callback=executor.submit(() -> catchThrowable(() -> {
                    if(success) jobs.succeed(token,pending.manifest(),callerNow);
                    else jobs.fail(token,"expired while waiting",callerNow);
                }));
                long waitDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while(jdbc.queryForObject("select count(*) from pg_stat_activity where wait_event_type='Lock' and query like '%reading_image_jobs%'",Integer.class)==0) {
                    if(System.nanoTime()>waitDeadline) fail("Callback never reached PostgreSQL row lock wait");
                    Thread.sleep(10);
                }
                Thread.sleep(Math.max(0,expiry-System.currentTimeMillis())+40);
                locker.commit();
                assertThat(callback.get(3,TimeUnit.SECONDS)).isInstanceOf(ReadingAttemptRejectedException.class).hasMessageContaining("NON_OWNING_TERMINAL_CALLBACK");
            }
            assertThat(jobs.find(id).state()).isEqualTo(State.PROCESSING);
            assertThat(token(id).lease()).isEqualTo(initial.lease());
            assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
        }
    }
    @Test void expiredUnreplacedProcessingTokenRejectsBothFirstTerminalCallbacks() throws Exception {
        var pending=pendingWithVerifiedManifest();var token=jobs.claim(pending.job().jobId(),System.currentTimeMillis()).orElseThrow();
        assertThatThrownBy(() -> jobs.succeed(token,pending.manifest(),token.expiresEpochMillis())).hasMessageContaining("NON_OWNING_TERMINAL_CALLBACK");
        assertThatThrownBy(() -> jobs.fail(token,"expired first failure",token.expiresEpochMillis())).hasMessageContaining("NON_OWNING_TERMINAL_CALLBACK");
        assertThat(jobs.find(token.jobId()).state()).isEqualTo(State.PROCESSING);
        assertThat(token(token.jobId())).isEqualTo(token);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
    }
    @Test void finalCaptureAndFinalBindingRecheckCannotSchedulePastTotalBudget() throws Exception {
        var profile=budget(500);ReadingProfiles.capture(Role.READING_PAGE,profile);
        for(boolean slowCapture:List.of(true,false)) {
            var creates=new AtomicInteger();var observes=new AtomicInteger();
            var slow=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
                @Override public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit,long millis) throws IOException {
                    var result=super.create(provider,path,mime,bytes,limit,millis);
                    if(slowCapture && creates.incrementAndGet()==3) sleep(600);return result;
                }
                @Override public VersionedStorageObject observe(StorageProvider provider,String path,long millis) {
                    var result=super.observe(provider,path,millis);
                    if(!slowCapture && observes.incrementAndGet()==6) sleep(600);return result;
                }
            };
            assertThatThrownBy(() -> service(slow).prepare(ReadingFixtures.snapshot(),profiles(profile))).hasMessageContaining("REVISION_DURATION_LIMIT");
            assertThat(jdbc.queryForObject("select count(*) from reading_image_jobs",Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
        }
    }
    @Test void readyFindUsesOneTotalBudgetAcrossAllPersistedOutputs() throws Exception {
        var profile=budget(500);var prepared=service.prepare(ReadingFixtures.snapshot(),profiles(profile));fastWorker(disk.storage()).run(prepared.jobId());
        var ready=jobs.find(prepared.jobId());assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var reads=new AtomicInteger();var remaining=new ArrayList<Long>();
        var slow=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public byte[] read(StorageProvider provider,VersionedStorageObject object,long limit,long millis) throws IOException {
                if(object.path().contains("/renditions/")) { reads.incrementAndGet();remaining.add(millis);sleep(200); }
                return super.read(provider,object,limit,millis);
            }
        };
        assertThatThrownBy(() -> service(slow).find(prepared.jobId())).hasMessageContaining("READING_VERIFICATION_INCONCLUSIVE").hasRootCauseMessage("REVISION_DURATION_LIMIT");
        assertThat(reads.get()).isEqualTo(3);
        assertThat(remaining.get(2)).isLessThan(remaining.get(0));
        assertThat(jobs.find(prepared.jobId()).manifest()).isEqualTo(ready.manifest());
    }
    @Test void workerTotalBudgetExpiryRecordsOwnedFailureAndReleasesParentCapacity() throws Exception {
        var profile=budget(500);var prepared=service.prepare(ReadingFixtures.snapshot(),profiles(profile));var writes=new AtomicInteger();
        var slow=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit,long millis) throws IOException {
                var result=super.create(provider,path,mime,bytes,limit,millis);
                if(path.contains("/renditions/") && writes.incrementAndGet()==3) sleep(600);return result;
            }
        };
        fastWorker(slow).run(prepared.jobId());
        var failed=jobs.find(prepared.jobId());assertThat(failed.state()).isEqualTo(State.FAILED);assertThat(failed.diagnostic()).contains("REVISION_DURATION_LIMIT");
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
        try(var capacity=ReadingPreparationCapacity.acquire()) { assertThat(capacity).isNotNull(); }
    }
    @Test void currentOriginalOrRegistryBindingChangingInsideRetainedCreateCannotScheduleStaleCapture() throws Exception {
        var prior=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC()).run(prior.jobId());var ready=service.find(prior.jobId());
        for(boolean pathChange:List.of(false,true)) {
            var created=new CountDownLatch(1);var resume=new CountDownLatch(1);var first=new AtomicBoolean(true);
            var blocked=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
                @Override public VersionedStorageObject create(StorageProvider provider,String path,String mime,byte[] bytes,long limit,long millis) throws IOException {
                    var stored=super.create(provider,path,mime,bytes,limit,millis);
                    if(first.getAndSet(false)) { created.countDown();try { if(!resume.await(3,TimeUnit.SECONDS)) throw new IOException("test barrier timeout"); }
                        catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IOException(e); } }
                    return stored;
                }
            };
            try(var executor=Executors.newSingleThreadExecutor()) {
                var capture=executor.submit(() -> catchThrowable(() -> service(blocked).prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles())));
                assertThat(created.await(3,TimeUnit.SECONDS)).isTrue();
                if(pathChange) {
                    disk.createImmutable("original/rebound-cover","image/png",Files.readAllBytes(disk.file("original/cover")));
                    when(assets.findById(3L)).thenReturn(Optional.of(MediaAsset.register(StorageProvider.FIREBASE_STORAGE,"original/rebound-cover",MediaKind.ORIGINAL_IMAGE)));
                } else {
                    var original=disk.objects.get("original/cover");
                    disk.objects.put(original.path(),new VersionedStorageObject(original.path(),UUID.randomUUID().toString(),original.mime(),original.bytes()));
                }
                resume.countDown();
                assertThat(capture.get(3,TimeUnit.SECONDS)).hasMessageContaining("SOURCE_BINDING_CHANGED_DURING_CAPTURE");
            }
            assertThat(jdbc.queryForObject("select count(*) from reading_image_jobs",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isEqualTo(1);
            assertThat(service.find(prior.jobId()).manifest()).isEqualTo(ready.manifest());
        }
    }
    @Test void jpegOriginalCompletesThroughCaptureActualChildAndIndependentStoredPngPixelDecode() throws Exception {
        byte[] jpeg=ReadingFixtures.quadrants("JPEG");
        for(String path:List.of("original/cover","original/page")) {
            disk.objects.remove(path);Files.delete(disk.file(path));disk.createImmutable(path,"image/jpeg",jpeg);
        }
        var job=service.prepare(ReadingFixtures.snapshot(),ReadingFixtures.profiles());
        new ReadingImageWorker(service,jobs,disk.storage(),Clock.systemUTC()).run(job.jobId());var ready=service.find(job.jobId());
        assertThat(ready.state()).withFailMessage("%s",ready.diagnostic()).isEqualTo(State.COMPLETED);
        var manifest=new ObjectMapper().readValue(ready.manifest(),Manifest.class);
        for(var source:jobs.work(job.jobId()).sources()) {
            assertThat(source.original().mime()).isEqualTo("image/jpeg");assertThat(source.sha256()).isEqualTo(ReadingIdentity.sha(jpeg));
        }
        for(var output:manifest.outputs()) {
            byte[] persisted=disk.storage().read(output.provider(),output.stored(),1_000_000);
            var decoded=javax.imageio.ImageIO.read(new ByteArrayInputStream(persisted));
            assertThat(decoded.getWidth()).isEqualTo(40);assertThat(decoded.getHeight()).isEqualTo(30);ReadingFixtures.assertQuadrants(decoded,12);
        }
    }
    @Test void readCommittedCallerStillGetsAnIndependentRepeatableReadCommittedSnapshot() throws Exception {
        long contentId=jdbc.queryForObject("insert into contents(type,external_key,page_count) values('STORY',?,1) returning id",Long.class,"p2-guard-"+UUID.randomUUID());
        jdbc.update("insert into content_localizations(content_id,language_code,title,cover_media_id) values(?,'tr','Fixture',3)",contentId);
        long page=jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,1) returning id",Long.class,contentId);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,illustration_media_id) values(?,'tr',4)",page);
        var isolation=new AtomicReference<Integer>();
        doAnswer(invocation -> {
            isolation.set(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
            var root=invocation.callRealMethod();
            try(var executor=Executors.newSingleThreadExecutor()) {
                executor.submit(() -> jdbc.update("update content_localizations set cover_media_id=4 where content_id=?",contentId)).get(3,TimeUnit.SECONDS);
            }
            return root;
        }).when(contents).findById(contentId);
        var caller=new TransactionTemplate(manager);caller.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            caller.execute(status -> {
                assertThat(jdbc.queryForObject("show transaction_isolation",String.class)).isEqualTo("read committed");
                var captured=snapshots.capture(contentId,"tr");captured.requireComplete();
                assertThat(isolation.get()).isEqualTo(java.sql.Connection.TRANSACTION_REPEATABLE_READ);
                assertThat(captured.bindings().getFirst().assetId()).isEqualTo(3);
                assertThat(jdbc.queryForObject("select cover_media_id from content_localizations where content_id=?",Long.class,contentId)).isEqualTo(4);
                assertThat(jdbc.queryForObject("show transaction_isolation",String.class)).isEqualTo("read committed");
                return null;
            });
        } finally { reset(contents);jdbc.update("delete from contents where id=?",contentId); }
    }
    @Test void parentPrepareRejectsExcessiveActualHeapReservationBeforeAnySourceIo() {
        var p=ReadingFixtures.profile();var excessive=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),Runtime.getRuntime().maxMemory(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,"synthetic excessive parent admission");
        var observations=new AtomicInteger();
        var checked=new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(disk))) {
            @Override public VersionedStorageObject observe(StorageProvider provider,String path,long millis) {
                observations.incrementAndGet();return super.observe(provider,path,millis);
            }
        };
        assertThatThrownBy(() -> service(checked).prepare(ReadingFixtures.snapshot(),profiles(excessive))).hasMessageContaining("PARENT_HEAP_ADMISSION_EXCEEDED");
        assertThat(observations.get()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from reading_image_jobs",Integer.class)).isZero();
    }
    @Test void scheduleAndSuccessCannotCommitWhenTotalBudgetExpiresInPostgresLockWait() throws Exception {
        var pending=pendingWithVerifiedManifest();var work=jobs.work(pending.job().jobId());UUID id=pending.job().jobId();
        var scheduleFailure=afterLockWait(id,() -> jobs.schedule(work,ReadingDeadline.start(250)));
        assertThat(scheduleFailure).isInstanceOf(ReadingDeadline.ReadingDeadlineExceededException.class);
        assertThat(jobs.find(id).state()).isEqualTo(State.PENDING);
        var token=jobs.claim(id,System.currentTimeMillis()).orElseThrow();
        var successFailure=afterLockWait(id,() -> jobs.succeed(token,pending.manifest(),System.currentTimeMillis(),ReadingDeadline.start(250)));
        assertThat(successFailure).isInstanceOf(ReadingDeadline.ReadingDeadlineExceededException.class);
        assertThat(jobs.find(id).state()).isEqualTo(State.PROCESSING);
        assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests",Integer.class)).isZero();
    }
    private Throwable afterLockWait(UUID id,org.assertj.core.api.ThrowableAssert.ThrowingCallable callback) throws Exception {
        var executor=Executors.newSingleThreadExecutor();
        try(var locker=jdbc.getDataSource().getConnection()) {
            locker.setAutoCommit(false);
            try {
                try(var statement=locker.prepareStatement("select id from reading_image_jobs where id=? for update")) {
                    statement.setObject(1,id);statement.executeQuery().close();
                }
                var result=executor.submit(() -> catchThrowable(callback));
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while(jdbc.queryForObject("select count(*) from pg_stat_activity where wait_event_type='Lock' and query like '%reading_image_jobs%'",Integer.class)==0) {
                    if(System.nanoTime()>deadline) fail("Callback never reached row lock wait");Thread.sleep(10);
                }
                Thread.sleep(350);locker.commit();return result.get(3,TimeUnit.SECONDS);
            } finally { locker.rollback(); }
        } finally { executor.shutdownNow();assertThat(executor.awaitTermination(3,TimeUnit.SECONDS)).isTrue(); }
    }
    private static void sleep(long millis) {
        try { Thread.sleep(millis); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
    }
}
