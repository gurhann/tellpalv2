package com.tellpal.v2.support;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.MediaAssetRepository;
import com.tellpal.v2.asset.domain.reading.ReadingJobStore;
import com.tellpal.v2.asset.infrastructure.persistence.reading.JdbcReadingSourceAdmissionStore;
import com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageFirebaseProperties;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.application.*;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.application.publication.*;
import com.tellpal.v2.content.domain.ContentRepository;
import com.tellpal.v2.content.infrastructure.persistence.publication.JdbcReadingPublicationStore;
import com.tellpal.v2.shared.domain.LanguageCode;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Gerçek helper komutları, PostgreSQL ve disk nesneleriyle admission'dan LIVE'a ilerler. */
@SpringBootTest(properties = {"tellpal.content.reading-publication.poll-delay=3600000"})
class P3LocalCmsSeedIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired MediaAssetRepository media;
    @Autowired AssetRegistryApi assets;
    @Autowired ContentRepository contentRepository;
    @Autowired ContentManagementService contents;
    @Autowired StoryPageManagementService pages;
    @Autowired JdbcReadingSourceAdmissionStore admissionsStore;
    @Autowired ReadingJobStore jobs;
    @Autowired JdbcReadingPublicationStore publicationsStore;
    @Autowired ReadingImageSnapshotAssembler assembler;
    @Autowired ReadingPublicationProperties config;
    @Autowired ReadingPublicationMutationGuard guard;
    @TempDir Path directory;

    @Test
    void actualAdmissionAndSeedBindNewAssetToDraftCoverAndPageThenPublishVerifiedLive() throws Exception {
        var disk = new ReadingFixtures.DiskStorage(directory.resolve("objects"));
        String prefix = "local/p3-cms-" + UUID.randomUUID();
        String path = prefix + "/fixture.png";
        byte[] originalBytes = ReadingFixtures.image(80, 60);
        disk.createImmutable(path, "image/png", originalBytes);
        long originalId = assets.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,
                path, AssetKind.ORIGINAL_IMAGE, "image/png", (long) originalBytes.length, null)).assetId();
        var env = new MockEnvironment();
        env.setActiveProfiles("test");
        var storageProperties = new AssetStorageFirebaseProperties(true, "synthetic", "synthetic", "unused", prefix, null, null, null);
        var admissions = new ReadingSourceAdmissionService(media, disk.storage(), admissionsStore, storageProperties, env, true);
        var preparation = new ReadingImagePreparationService(media, disk.storage(), jobs, env, storageProperties, true, admissions);
        var ready = new ReadingPublicationReadyService(preparation, admissions, jobs, jdbc, manager, disk.storage());
        var profile = P3LocalCmsSetup.profile();
        config.setEnabled(true);
        config.setProfiles(Map.of(Role.READING_COVER, profile, Role.READING_DETAIL, profile, Role.READING_PAGE, profile));
        try {
            var receipt = P3LocalCmsSetup.Receipt.create(directory.resolve("receipt.properties"));
            // Uygulanan araç da bu aynı admission→seed metodunu çağırır; ayrı SQL seed yoktur.
            var seeded = P3LocalCmsSetup.admitAndSeed(admissions, originalId, contents, pages, assets, prefix, receipt);
            long contentId = seeded.contentId();
            long admittedId = seeded.admission().assetId();
            assertThat(admittedId).isNotEqualTo(originalId);
            assertThat(jdbc.queryForObject("select cover_media_id from content_localizations where content_id=? and language_code='tr'", Long.class, contentId))
                    .isEqualTo(admittedId);
            assertThat(jdbc.queryForObject("select spl.illustration_media_id from story_page_localizations spl join story_pages sp on sp.id=spl.story_page_id where sp.content_id=? and sp.page_number=1 and spl.language_code='tr'", Long.class, contentId))
                    .isEqualTo(admittedId);
            assertThat(jdbc.queryForList("select language_code || ':' || status from content_localizations where content_id=?", String.class, contentId))
                    .containsExactly("tr:DRAFT");
            assertThat(jdbc.queryForObject("select count(*) from story_pages where content_id=?", Integer.class, contentId)).isEqualTo(1);
            assertThat(media.findById(originalId)).isPresent();
            assertThat(java.nio.file.Files.readAllBytes(disk.file(path))).isEqualTo(originalBytes);

            var publication = new ReadingPublicationService(config, guard, contentRepository, publicationsStore, assembler, ready, manager);
            var publishing = new ContentPublicationService(contentRepository, mock(AssetProcessingApi.class), publication, guard);
            var tx = new TransactionTemplate(manager);
            tx.executeWithoutResult(status -> publishing.publishLocalization(new PublishContentLocalizationCommand(contentId, LanguageCode.from("tr"), Instant.now())));
            assertThat(publication.status(contentId, "tr").liveRevision()).isNull();
            publication.reconcile(contentId, "tr");
            UUID jobId = jdbc.queryForObject("select job_id from reading_publications where content_id=? and language_code='tr'", UUID.class, contentId);
            new ReadingImageWorker(preparation, jobs, disk.storage(), Clock.systemUTC()).run(jobId);
            publication.reconcile(contentId, "tr");
            var live = publication.status(contentId, "tr");
            assertThat(live.state()).isEqualTo("LIVE");
            assertThat(live.liveRevision()).matches("[0-9a-f]{64}");
            assertThat(live.liveManifestDigest()).matches("[0-9a-f]{64}");
            assertThat(jobs.work(jobId).sources()).hasSize(3).allMatch(source -> source.binding().assetId() == admittedId);
            assertThat(jdbc.queryForObject("select status from content_localizations where content_id=? and language_code='tr'", String.class, contentId))
                    .isEqualTo("PUBLISHED");
        } finally {
            config.setEnabled(false);
        }
    }
}
