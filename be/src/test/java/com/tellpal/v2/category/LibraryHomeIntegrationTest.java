package com.tellpal.v2.category;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.time.Instant;
import java.time.Clock;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.category.api.*;
import com.tellpal.v2.category.application.*;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.AssetProcessingCommands.*;
import com.tellpal.v2.asset.application.*;
import com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyRunningException;
import com.tellpal.v2.asset.infrastructure.processing.AssetProcessingJobExecutor;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProcessingExecutor;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProfile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.shared.domain.LanguageCode;

@SpringBootTest(properties={"tellpal.asset.processing.poller.enabled=false"})
class LibraryHomeIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired LibraryHomeQueryApi library;
    @Autowired CategoryHomeConfigurationService home;
    @Autowired LibraryCoverPreparationApi preparation;
    @Autowired AssetProcessingApi jobs;
    @Autowired AssetProcessingJobExecutor executor;
    @Autowired LibraryCoverQueryApi covers;
    @Autowired AssetRegistryApi assets;
    @MockitoSpyBean AssetStorageClientRegistry storage;
    @Autowired ListingCoverProcessingExecutor listingExecutor;
    @MockitoSpyBean ListingCoverProfile profile;
    @MockitoSpyBean Clock clock;
    @Autowired LibraryCoverPreparationService coverPreparation;
    @BeforeEach void reset() {jdbc.execute("truncate categories,contents,media_assets restart identity cascade");}
    @Test void savedIndependentOrdersFilterBeforeFiveAndNeverPadTurkishWithEnglish() {
        long alpha=category("alpha","Alpha"),zeta=category("zeta","English Stories"),empty=category("empty","Empty"),promo=category("promo","Try Premium");
        long hidden=category("hidden","Hidden");jdbc.update("update categories set is_active=false where id=?",hidden);
        for(int i=0;i<9;i++){long id=story("large-"+i,"tr",i!=0);link(alpha,id,"tr",i);}
        for(int i=0;i<3;i++){long id=story("tr-"+i,"tr",true);link(zeta,id,"tr",i);}
        for(int i=0;i<2;i++){long id=story("en-only-"+i,"en",true);link(zeta,id,"en",i);}
        long free=jdbc.queryForObject("select min(content_id) from category_contents where category_id=? and language_code='tr' and display_order>0",Long.class,alpha);
        jdbc.update("insert into content_free_access(access_key,content_id,language_code) values('default',?,'tr')",free);
        link(promo,free,"tr",0);
        var tr=home.read("tr");home.save("tr",tr.revision(),tr.rows().stream().map(r->config(r.categoryId(), r.categoryId()==alpha?2:r.categoryId()==zeta?1:r.categoryId()==empty?0:r.categoryId()==hidden?3:4,r.categoryId()==promo?"NON_SUBSCRIBER":"ALL",null)).toList());
        var en=home.read("en");home.save("en",en.revision(),en.rows().stream().map(r->config(r.categoryId(),r.categoryId()==alpha?0:r.categoryId()==zeta?1:r.categoryId()==empty?2:r.categoryId()==hidden?3:4,"ALL",null)).toList());
        var initial=library.query("TR",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);
        assertThat(initial.previewLimit()).isEqualTo(5);
        assertThat(initial.rows()).extracting(LibraryHomeCatalog.Row::categoryId).containsExactly(zeta,alpha,promo);
        assertThat(initial.rows().getFirst().items()).hasSize(3).allSatisfy(i->assertThat(i.contentLanguage()).isEqualTo("tr"));
        assertThat(initial.rows().get(1).items()).hasSize(5);
        assertThat(initial.rows().get(1).items().getFirst().contentId()).isEqualTo(free);
        assertThat(initial.rows().get(1).items().getFirst().isFree()).isTrue();
        assertThat(initial.rows().get(1).items().get(1).isLocked()).isTrue();
        var englishBefore=home.read("en");var cardOrder=initial.rows().get(1).items();
        var current=home.read("tr");var saved=home.save("tr",current.revision(),current.rows().stream().map(r->config(r.categoryId(),r.categoryId()==alpha?1:r.categoryId()==zeta?2:r.displayOrder(),r.audience(),r.contentLanguage())).toList());
        assertThat(home.read("tr")).isEqualTo(saved);
        assertThat(home.read("en")).isEqualTo(englishBefore);
        var reordered=library.query("tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);
        assertThat(reordered.rows()).extracting(LibraryHomeCatalog.Row::categoryId).containsExactly(alpha,zeta,promo);
        assertThat(reordered.rows().getFirst().items()).isEqualTo(cardOrder);
        var subscribers=library.query("tr",LibraryHomeQueryApi.Audience.SUBSCRIBER);
        assertThat(subscribers.rows()).extracting(LibraryHomeCatalog.Row::categoryId).containsExactly(alpha,zeta);
        assertThat(subscribers.rows()).flatExtracting(LibraryHomeCatalog.Row::items).allSatisfy(i->assertThat(i.isLocked()).isFalse());
        home.save(zeta,"tr",config(zeta,2,"ALL","en"));
        var mapped=library.query("tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().get(1);
        assertThat(mapped.name()).isEqualTo("English Stories");assertThat(mapped.contentLanguage()).isEqualTo("en");
        assertThat(mapped.items()).hasSize(2).allSatisfy(i->assertThat(i.title()).startsWith("en:"));
        jdbc.update("update category_localizations set status='DRAFT' where category_id=? and language_code='en'",zeta);
        assertThat(library.query("tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows()).extracting(LibraryHomeCatalog.Row::categoryId).doesNotContain(zeta);
    }
    @Test void eligibilityFilteringContinuesPastAFullHundredCandidateBatchBeforeTheLimit() {
        long row = category("late-eligible", "Late eligible stories");
        long inactive = story("early-inactive", "tr", true);
        jdbc.update("update contents set is_active=false where id=?", inactive);
        link(row, inactive, "tr", 0);
        long draft = story("early-draft", "tr", true);
        jdbc.update("update content_localizations set status='DRAFT', published_at=null where content_id=?", draft);
        link(row, draft, "tr", 1);
        for (int index = 0; index < 100; index++) {
            link(row, story("unready-" + index, "tr", false), "tr", index + 2);
        }
        List<Long> eligible = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            long id = story("eligible-" + index, "tr", true);
            eligible.add(id);
            link(row, id, "tr", 102 + index);
        }
        home.save(row, "tr", config(row, 0, "ALL", null));
        var result = library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);
        assertThat(result.rows()).extracting(LibraryHomeCatalog.Row::categoryId).containsExactly(row);
        assertThat(result.previewLimit()).isEqualTo(5);
        assertThat(result.rows().getFirst().items()).extracting(com.tellpal.v2.content.api.ContentPreview::contentId)
                .containsExactlyElementsOf(eligible.subList(0, 5));
    }
    @Test void staleDuplicateAndInvalidMappingWritesAreAtomicAndExistingRowsStartOptedOut() {
        long a=category("a","A"),b=category("b","B");
        assertThat(library.query("tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows()).isEmpty();
        var initial=home.read("tr");var configured=List.of(config(a,0,"ALL",null),config(b,1,"ALL",null));
        home.save("tr",initial.revision(),configured);
        assertThatThrownBy(()->home.save("tr",initial.revision(),configured)).isInstanceOf(CategoryHomeConfigurationService.Conflict.class);
        var current=home.read("tr");assertThatThrownBy(()->home.save("tr",current.revision(),List.of(config(a,0,"ALL",null),config(b,0,"ALL",null)))).isInstanceOf(IllegalArgumentException.class);
        assertThat(home.read("tr")).isEqualTo(current);
        assertThatThrownBy(()->home.save(a,"tr",config(a,1,"ALL",null))).isInstanceOf(CategoryHomeConfigurationService.Conflict.class);
        jdbc.update("delete from category_localizations where category_id=? and language_code='pt'",a);
        assertThatThrownBy(()->home.save(a,"tr",config(a,0,"ALL","pt"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->jdbc.update("update category_localizations set home_display_order=-1 where category_id=?",a)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void anAlreadyLoadedJpaLocalizationEditCannotOverwriteAtomicHomeConfiguration() {
        long id=category("concurrent","Original");
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status->{
            var localization=entityManager.createQuery("select l from CategoryLocalization l where l.category.id=:id and l.languageCode=:language",com.tellpal.v2.category.domain.CategoryLocalization.class).setParameter("id",id).setParameter("language",LanguageCode.TR).getSingleResult();
            assertThat(localization.getHomeDisplayOrder()).isNull();
            home.save(id,"tr",config(id,0,"NON_SUBSCRIBER","en"));
            localization.updateContent("Edited after home save",null,null);
            entityManager.flush();
        });
        var saved=home.read(id,"tr");
        assertThat(saved.name()).isEqualTo("Edited after home save");
        assertThat(saved.displayOrder()).isZero();assertThat(saved.audience()).isEqualTo("NON_SUBSCRIBER");assertThat(saved.contentLanguage()).isEqualTo("en");assertThat(saved.version()).isEqualTo(1);
        assertThat(home.read(id,"en").displayOrder()).isNull();
    }
    @Test void realBytePreparationAndLateCallbacksPreserveTheReplacementAndReadyManifest() throws Exception {
        long id=story("covered","tr",true);long source=image("first",80,100);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?",source,id);
        var target=AssetProcessingTarget.localization(id,LanguageCode.TR);
        var scheduled=preparation.prepare(target,"covered",source);assertThat(scheduled.ready()).isFalse();
        var a=jobs.start(new StartAssetProcessingCommand(target,AssetProcessingKind.LISTING_COVER));
        jdbc.update("update asset_processing set lease_expires_at=? where id=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),a.processingId());
        jobs.recoverExpiredLease(new RecoverExpiredAssetProcessingCommand(target,AssetProcessingKind.LISTING_COVER));
        var b=jobs.start(new StartAssetProcessingCommand(target,AssetProcessingKind.LISTING_COVER));
        assertThatThrownBy(()->jobs.complete(new CompleteAssetProcessingCommand(target,a.kind(),ListingCoverAttempt.captured(a)))).isInstanceOf(StaleListingCoverAttemptException.class);
        assertThatThrownBy(()->jobs.fail(new FailAssetProcessingCommand(target,a.kind(),"FAILED","Stale failure",ListingCoverAttempt.captured(a)))).isInstanceOf(StaleListingCoverAttemptException.class);
        executor.process(a); // Rejected publication returns without a catch failing B.
        assertThat(jobs.findByTarget(target,a.kind()).orElseThrow().attemptCount()).isEqualTo(b.attemptCount());
        assertThat(jobs.findByTarget(target,a.kind()).orElseThrow().status()).isEqualTo(AssetProcessingState.PROCESSING);
        assertThat(covers.find(id,"tr",source)).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        executor.process(b);
        var ready=covers.find(id,"tr",source);assertThat(ready.phone()).isNotNull();assertThat(ready.tablet()).isNotNull();
        var phone=assets.findById(ready.phone().assetId()).orElseThrow();
        var object=storage.openObject(StorageProvider.FIREBASE_STORAGE,phone.storageLocation().objectPath(),null).orElseThrow();
        try(var input=object.content()){var decoded=ImageIO.read(input);assertThat(decoded.getWidth()).isEqualTo(80);assertThat(decoded.getHeight()).isEqualTo(100);}
        assertThat(preparation.prepare(target,"covered",source).ready()).isTrue();
        executor.process(a);assertThat(covers.find(id,"tr",source).phone().assetId()).isEqualTo(ready.phone().assetId());
        long replacement=image("second",1000,1500);jdbc.update("update content_localizations set cover_media_id=? where content_id=?",replacement,id);
        assertThat(covers.find(id,"tr",replacement).phone()).isNull();
        preparation.prepare(target,"covered",replacement);executor.process(jobs.start(new StartAssetProcessingCommand(target,a.kind())));
        var next=covers.find(id,"tr",replacement);assertThat(next.phone().assetId()).isNotEqualTo(ready.phone().assetId());
        assertThat(jdbc.queryForObject("select count(*) from listing_cover_revisions",Integer.class)).isEqualTo(2);
    }
    @Test void currentSecondVariantUploadFailurePersistsFailedWithoutPublishingAndExplicitRetrySucceeds() throws Exception {
        org.mockito.Mockito.doReturn(Instant.parse("2026-10-06T08:00:00Z")).when(clock).instant();
        long row = category("current-failure", "Current failure stories");
        long id = story("current-failure", "tr", true);
        long originalSource = image("current-failure-original", 80, 100);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", originalSource, id);
        link(row, id, "tr", 0);
        home.save(row, "tr", config(row, 0, "ALL", null));
        var target = AssetProcessingTarget.localization(id, LanguageCode.TR);
        preparation.prepare(target, "current-failure", originalSource);
        listingExecutor.process(jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER)));
        var original = covers.find(id, "tr", originalSource);
        assertThat(original.phone()).isNotNull();
        assertThat(original.tablet()).isNotNull();
        var manifests = jdbc.queryForList("select * from listing_cover_revisions");

        long source = image("current-failure-replacement", 1000, 1500);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", source, id);
        preparation.prepare(target, "current-failure", source);
        var worker = jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER));
        assertThat(worker.status()).isEqualTo(AssetProcessingState.PROCESSING);
        assertThat(worker.leaseExpiresAt()).isAfter(clock.instant());
        var uploadPaths = new ArrayList<String>();
        var failTablet = new java.util.concurrent.atomic.AtomicBoolean(true);
        org.mockito.Mockito.doAnswer(invocation -> {
            String path = invocation.getArgument(1);
            uploadPaths.add(path);
            if (path.endsWith("/tablet.jpg") && failTablet.getAndSet(false)) {
                throw new IllegalStateException("Injected second listing variant upload failure");
            }
            return invocation.callRealMethod();
        }).when(storage).uploadObject(org.mockito.ArgumentMatchers.eq(StorageProvider.FIREBASE_STORAGE),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("image/jpeg"),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(InputStream.class));
        listingExecutor.process(worker);
        assertThat(uploadPaths).hasSize(2);
        assertThat(uploadPaths.getFirst()).endsWith("/phone.jpg");
        assertThat(uploadPaths.getLast()).endsWith("/tablet.jpg");
        assertThat(storage.findObjectMetadata(StorageProvider.FIREBASE_STORAGE, uploadPaths.getFirst())).isPresent();
        var failed = jobs.findByTarget(target, worker.kind()).orElseThrow();
        assertThat(failed.processingId()).isEqualTo(worker.processingId());
        assertThat(failed.attemptCount()).isEqualTo(worker.attemptCount());
        assertThat(failed.coverSourceAssetId()).isEqualTo(source);
        assertThat(failed.listingProfileVersion()).isEqualTo(worker.listingProfileVersion());
        assertThat(failed.status()).isEqualTo(AssetProcessingState.FAILED);
        assertThat(failed.lastErrorCode()).isEqualTo("LISTING_COVER_FAILED");
        assertThat(failed.lastErrorMessage()).isEqualTo("Listing image preparation failed");
        assertThat(failed.failedAt()).isNotNull();
        assertThat(failed.leaseExpiresAt()).isNull();
        assertThat(jdbc.queryForMap("select status,last_error_code,last_error_message from asset_processing where id=?", worker.processingId()))
                .containsEntry("status", "FAILED").containsEntry("last_error_code", "LISTING_COVER_FAILED")
                .containsEntry("last_error_message", "Listing image preparation failed");
        assertThat(jdbc.queryForList("select * from listing_cover_revisions")).isEqualTo(manifests);
        assertThat(covers.find(id, "tr", source)).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        assertThat(library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().getFirst().items().getFirst().covers())
                .isEqualTo(LibraryCoverQueryApi.Covers.empty());
        assertThat(covers.find(id, "tr", originalSource)).isEqualTo(original);

        var retry = jobs.retry(new RetryAssetProcessingCommand(target, worker.kind(), AssetProcessingContentType.STORY,
                "current-failure", source, null, 0));
        assertThat(retry.status()).isEqualTo(AssetProcessingState.PENDING);
        var current = jobs.start(new StartAssetProcessingCommand(target, worker.kind()));
        assertThat(current.attemptCount()).isEqualTo(worker.attemptCount() + 1);
        assertThat(current.coverSourceAssetId()).isEqualTo(source);
        assertThat(current.listingProfileVersion()).isEqualTo(worker.listingProfileVersion());
        listingExecutor.process(current);
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow().status()).isEqualTo(AssetProcessingState.COMPLETED);
        var ready = covers.find(id, "tr", source);
        for (var variant : List.of(ready.phone(), ready.tablet())) {
            var asset = assets.findById(variant.assetId()).orElseThrow();
            assertThat(asset.mimeType()).isEqualTo("image/jpeg");
            var object = storage.openObject(StorageProvider.FIREBASE_STORAGE, asset.storageLocation().objectPath(), null).orElseThrow();
            try (var input = object.content()) {
                var decoded = ImageIO.read(input);
                assertThat(decoded).isNotNull();
                assertThat(decoded.getWidth()).isPositive();
                assertThat(decoded.getHeight()).isPositive();
            }
        }
        assertThat(library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().getFirst().items().getFirst().covers()).isEqualTo(ready);
        assertThat(jdbc.queryForObject("select count(*) from listing_cover_revisions", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("select * from listing_cover_revisions where source_asset_id=?", originalSource)).isEqualTo(manifests);
        assertThat(covers.find(id, "tr", originalSource)).isEqualTo(original);
    }
    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void persistedCallbacksAtAndAfterLeaseExpiryBeforeRecoveryPreserveTheExactJobAndReadyManifest(int secondsAfterExpiry) throws Exception {
        Instant startedAt = Instant.parse("2026-10-06T08:00:00Z");
        org.mockito.Mockito.doReturn(startedAt).when(clock).instant();
        long id = story("expiry-before-recovery", "tr", true);
        long originalSource = image("expiry-original", 80, 100);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", originalSource, id);
        var target = AssetProcessingTarget.localization(id, LanguageCode.TR);
        preparation.prepare(target, "expiry-before-recovery", originalSource);
        executor.process(jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER)));
        var ready = covers.find(id, "tr", originalSource);
        assertThat(ready.phone()).isNotNull();
        var manifests = jdbc.queryForList("select * from listing_cover_revisions");

        long source = image("expiry-pending", 100, 150);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", source, id);
        preparation.prepare(target, "expiry-before-recovery", source);
        jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER));
        var worker = jobs.findByTarget(target, AssetProcessingKind.LISTING_COVER).orElseThrow();
        var captured = ListingCoverAttempt.captured(worker);
        var before = jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId());
        assertThat(worker.status()).isEqualTo(AssetProcessingState.PROCESSING);
        assertThat(worker.leaseExpiresAt()).isAfter(startedAt);
        org.mockito.Mockito.doReturn(worker.leaseExpiresAt().plusSeconds(secondsAfterExpiry)).when(clock).instant();
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow()).isEqualTo(worker);

        assertThatThrownBy(() -> jobs.complete(new CompleteAssetProcessingCommand(target, worker.kind(), captured)))
                .isInstanceOf(StaleListingCoverAttemptException.class);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(before);
        assertThatThrownBy(() -> jobs.fail(new FailAssetProcessingCommand(target, worker.kind(), "EXPIRED", "Expired worker", captured)))
                .isInstanceOf(StaleListingCoverAttemptException.class);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(before);
        var attemptedRevision = new ListingCoverRevisionStore.Revision(id, "tr", source,
                "0".repeat(64), worker.listingProfileVersion(), "1".repeat(64),
                ready.phone().assetId(), ready.tablet().assetId(), 80, 100, 80, 100);
        assertThatThrownBy(() -> coverPreparation.publish(worker, attemptedRevision))
                .isInstanceOf(StaleListingCoverAttemptException.class);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(before);
        assertThat(jdbc.queryForList("select * from listing_cover_revisions")).isEqualTo(manifests);
        executor.process(worker); // Its rejected publication/catch cannot fail the unchanged expired attempt.
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow()).isEqualTo(worker);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(before);
        assertThat(jdbc.queryForList("select * from listing_cover_revisions")).isEqualTo(manifests);
        assertThat(covers.find(id, "tr", source)).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        assertThat(covers.find(id, "tr", originalSource).phone().assetId()).isEqualTo(ready.phone().assetId());
        assertThat(covers.find(id, "tr", originalSource).tablet().assetId()).isEqualTo(ready.tablet().assetId());
    }
    @Test void theSameSourceRequiresSeparateProfilePreparationAndPublishesOnlyTheCurrentProfile() throws Exception {
        long row = category("profiles", "Profile revisions");
        long id = story("same-source-profiles", "tr", true);
        long source = image("same-source-profiles", 1000, 1500);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", source, id);
        link(row, id, "tr", 0);
        home.save(row, "tr", config(row, 0, "ALL", null));
        var target = AssetProcessingTarget.localization(id, LanguageCode.TR);
        String firstProfile = profile.version();
        preparation.prepare(target, "same-source-profiles", source);
        executor.process(jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER)));
        var first = covers.find(id, "tr", source);
        assertThat(first.phone()).isNotNull();
        assertThat(first.tablet()).isNotNull();
        String firstRevision = jdbc.queryForObject("select revision from listing_cover_revisions where profile_version=?", String.class, firstProfile);

        var secondProfile = new ListingCoverProfile(180, 240, 300, 400, 70, profile.maxSourceBytes(), profile.maxPixels());
        org.mockito.Mockito.doReturn(secondProfile.version()).when(profile).version();
        assertThat(covers.find(id, "tr", source)).isEqualTo(LibraryCoverQueryApi.Covers.empty());
        assertThat(library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().getFirst().items().getFirst().covers())
                .isEqualTo(LibraryCoverQueryApi.Covers.empty());
        var scheduled = preparation.prepare(target, "same-source-profiles", source);
        assertThat(scheduled.ready()).isFalse();
        assertThat(scheduled.operationId()).isNotNull();
        var secondJob = jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER));
        assertThat(secondJob.coverSourceAssetId()).isEqualTo(source);
        assertThat(secondJob.listingProfileVersion()).isEqualTo(secondProfile.version());
        executor.process(secondJob);
        var current = covers.find(id, "tr", source);
        assertThat(current.phone().assetId()).isNotEqualTo(first.phone().assetId());
        assertThat(current.tablet().assetId()).isNotEqualTo(first.tablet().assetId());
        var catalogCovers = library.query("tr", LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).rows().getFirst().items().getFirst().covers();
        assertThat(catalogCovers.phone().assetId()).isEqualTo(current.phone().assetId());
        assertThat(catalogCovers.tablet().assetId()).isEqualTo(current.tablet().assetId());
        String secondRevision = jdbc.queryForObject("select revision from listing_cover_revisions where profile_version=?", String.class, secondProfile.version());
        assertThat(secondRevision).isNotEqualTo(firstRevision);
        assertThat(jdbc.queryForObject("select count(*) from listing_cover_revisions where source_asset_id=?", Integer.class, source)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select phone_width from listing_cover_revisions where profile_version=?", Integer.class, secondProfile.version())).isEqualTo(160);
        assertThat(jdbc.queryForObject("select tablet_width from listing_cover_revisions where profile_version=?", Integer.class, secondProfile.version())).isEqualTo(267);
        assertThat(preparation.prepare(target, "same-source-profiles", source).revision()).isEqualTo(secondRevision);
        assertThat(jobs.findByTarget(target, AssetProcessingKind.LISTING_COVER).orElseThrow().status()).isEqualTo(AssetProcessingState.COMPLETED);
    }
    @Test void processingListingCoverRejectsChangedSourceAndProfileWithoutMutatingAnyJobField() throws Exception {
        long id = story("processing-conflict", "tr", true);
        long source = image("processing-conflict-original", 80, 100);
        long replacement = image("processing-conflict-replacement", 120, 150);
        jdbc.update("update content_localizations set cover_media_id=? where content_id=?", source, id);
        var target = AssetProcessingTarget.localization(id, LanguageCode.TR);
        String originalProfile = profile.version();
        preparation.prepare(target, "processing-conflict", source);
        jobs.start(new StartAssetProcessingCommand(target, AssetProcessingKind.LISTING_COVER));
        var worker = jobs.findByTarget(target, AssetProcessingKind.LISTING_COVER).orElseThrow();
        var entireJob = jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId());
        assertThat(worker.status()).isEqualTo(AssetProcessingState.PROCESSING);

        assertThatThrownBy(() -> preparation.prepare(target, "processing-conflict", replacement))
                .isInstanceOf(AssetProcessingAlreadyRunningException.class);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(entireJob);
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow()).isEqualTo(worker);
        var changedProfile = new ListingCoverProfile(180, 240, 300, 400, 70, profile.maxSourceBytes(), profile.maxPixels());
        org.mockito.Mockito.doReturn(changedProfile.version()).when(profile).version();
        assertThatThrownBy(() -> preparation.prepare(target, "processing-conflict", source))
                .isInstanceOf(AssetProcessingAlreadyRunningException.class);
        assertThat(jdbc.queryForMap("select * from asset_processing where id=?", worker.processingId())).isEqualTo(entireJob);
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow()).isEqualTo(worker);
        assertThat(jdbc.queryForObject("select count(*) from listing_cover_revisions", Integer.class)).isZero();

        org.mockito.Mockito.doReturn(originalProfile).when(profile).version();
        executor.process(worker);
        assertThat(jobs.findByTarget(target, worker.kind()).orElseThrow().status()).isEqualTo(AssetProcessingState.COMPLETED);
        var ready = covers.find(id, "tr", source);
        assertThat(ready.phone()).isNotNull();
        assertThat(ready.tablet()).isNotNull();
        assertThat(jdbc.queryForObject("select source_asset_id from listing_cover_revisions", Long.class)).isEqualTo(source);
        assertThat(jdbc.queryForObject("select profile_version from listing_cover_revisions", String.class)).isEqualTo(originalProfile);
    }
    private CategoryHomeConfigurationService.Configuration config(long id,Integer order,String audience,String language) {return new CategoryHomeConfigurationService.Configuration(id,order,audience,language);}
    private long category(String slug,String name) {
        long id=jdbc.queryForObject("insert into categories(slug,type) values(?,'STORY') returning id",Long.class,slug);
        for(String lang:List.of("tr","en","pt")) jdbc.update("insert into category_localizations(category_id,language_code,name,status,published_at) values(?,?,?,'PUBLISHED',now())",id,lang,name);
        return id;
    }
    private long story(String key,String language,boolean ready) {
        long id=jdbc.queryForObject("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY',?,true,5,0) returning id",Long.class,key);
        jdbc.update("insert into content_localizations(content_id,language_code,title,status,processing_status,published_at) values(?,?,?,'PUBLISHED',?,now())",id,language,language+":"+key,ready?"COMPLETED":"PENDING");return id;
    }
    private void link(long category,long content,String language,int order){jdbc.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,?,?)",category,content,language,order);}
    private long image(String name,int width,int height) throws Exception {
        var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB),"png",output);byte[] bytes=output.toByteArray();String path="test/library/"+name+".png";
        storage.uploadObject(StorageProvider.FIREBASE_STORAGE,path,"image/png",bytes.length,new ByteArrayInputStream(bytes));
        return assets.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,path,AssetKind.ORIGINAL_IMAGE,"image/png",(long)bytes.length,ListingCoverProcessingExecutor.sha256(bytes))).assetId();
    }
}
