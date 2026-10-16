package com.tellpal.v2.content.publication;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tellpal.v2.asset.api.ReadingSourceAdmissionApi;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.application.ContentPublicationService;
import com.tellpal.v2.content.application.ContentPublicationCommands.PublishContentLocalizationCommand;
import com.tellpal.v2.content.application.publication.*;
import com.tellpal.v2.shared.domain.LanguageCode;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

/** Exercises both real Spring scheduled polls, the encoder, pinned disk objects and PostgreSQL. */
@SpringBootTest(properties = {
        "tellpal.asset.reading.test-engine-enabled=true",
        "tellpal.asset.reading.poll-delay=100",
        "tellpal.content.reading-publication.poll-delay=100"})
@Import(ReadingPublicationSchedulerIntegrationTest.StorageConfiguration.class)
class ReadingPublicationSchedulerIntegrationTest extends PostgresIntegrationTestBase {
    record DiskFixture(ReadingFixtures.DiskStorage storage) { }

    @TestConfiguration
    static class StorageConfiguration {
        @Bean DiskFixture schedulerDisk() throws java.io.IOException {
            return new DiskFixture(new ReadingFixtures.DiskStorage(Files.createTempDirectory("p3-scheduler-")));
        }
        @Bean @Primary AssetStorageClientRegistry schedulerRegistry(DiskFixture disk) {
            return new AssetStorageClientRegistry(List.of(disk.storage()));
        }
    }

    @Autowired DiskFixture fixture;
    @Autowired MediaAssetRepository assets;
    @Autowired ReadingSourceAdmissionApi admissions;
    @Autowired ReadingPublicationProperties config;
    @Autowired ReadingPublicationService publication;
    @Autowired ContentPublicationService publishing;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;

    @Test void publishAutomaticallyReachesLiveThroughSpringScheduledReconcilerAndWorker() throws Exception {
        var disk = fixture.storage();
        var tx = new TransactionTemplate(manager);
        String path = "test/manual/scheduled-" + UUID.randomUUID();
        disk.createImmutable(path, "image/png", ReadingFixtures.image(80, 60));
        long source = tx.execute(s -> assets.save(MediaAsset.register(
                StorageProvider.FIREBASE_STORAGE, path, MediaKind.ORIGINAL_IMAGE)).getId());
        long admitted = admissions.admit(source, ReadingFixtures.profile()).assetId();
        long id = jdbc.queryForObject("insert into contents(type,external_key,page_count,is_active) values('STORY',?,1,true) returning id", Long.class, "scheduled-" + UUID.randomUUID());
        jdbc.update("insert into content_localizations(content_id,language_code,title,description,cover_media_id,status,processing_status) values(?,'tr','Scheduled','Description',?,'DRAFT','COMPLETED')", id, admitted);
        long page = jdbc.queryForObject("insert into story_pages(content_id,page_number) values(?,1) returning id", Long.class, id);
        jdbc.update("insert into story_page_localizations(story_page_id,language_code,body_text,audio_media_id,illustration_media_id) values(?,'tr','Page text',?,?)", page, source, admitted);
        config.setProfiles(ReadingFixtures.profiles());
        config.setEnabled(true);
        try {
            publishing.publishLocalization(new PublishContentLocalizationCommand(id, LanguageCode.from("tr"), Instant.now()));
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
            while (!publication.status(id, "tr").state().equals("LIVE") && System.nanoTime() < deadline) Thread.sleep(50);
            var status = publication.status(id, "tr");
            assertThat(status.state()).isEqualTo("LIVE");
            assertThat(status.liveRevision()).matches("[0-9a-f]{64}");
            assertThat(status.liveManifestDigest()).matches("[0-9a-f]{64}");
            assertThat(jdbc.queryForObject("select count(*) from reading_image_manifests", Integer.class)).isEqualTo(1);
            assertThat(disk.objects.keySet()).anyMatch(name -> name.contains("/reading/renditions/"));
        } finally {
            config.setEnabled(false);
        }
    }
}
