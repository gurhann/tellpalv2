package com.tellpal.v2.user.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.content.application.ContentManagementCommands.CreateContentCommand;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.domain.ContentType;
import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportStatus;
import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportSummary;
import com.tellpal.v2.event.infrastructure.firebase.migration.FirebaseEventMigrationJob;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportStatus;
import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportSummary;
import com.tellpal.v2.user.infrastructure.firebase.migration.FirebaseMigrationCommand;
import com.tellpal.v2.user.infrastructure.firebase.migration.FirebaseUserMigrationJob;

@SpringBootTest
class FirebaseMigrationIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ContentManagementService contentManagementService;

    @Autowired
    private FirebaseUserMigrationJob firebaseUserMigrationJob;

    @Autowired
    private FirebaseEventMigrationJob firebaseEventMigrationJob;

    @TempDir
    Path tempDir;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    purchase_context_snapshots,
                    purchase_events,
                    app_events,
                    content_events,
                    content_free_access,
                    category_contents,
                    category_localizations,
                    categories,
                    content_contributors,
                    contributors,
                    story_page_localizations,
                    story_pages,
                    content_localizations,
                    contents,
                    user_profiles,
                    app_users
                restart identity cascade
                """);
    }

    @Test
    void userDryRunReadsFixturesWithoutWritingDatabaseRows() throws Exception {
        FirebaseMigrationCommand dryRunCommand = command(true, prepareStagingDir("user-dry-run"));

        FirebaseUserImportSummary summary = firebaseUserMigrationJob.run(dryRunCommand);

        assertThat(summary.dryRun()).isTrue();
        assertThat(summary.processedCount()).isEqualTo(2);
        assertThat(summary.createdCount()).isEqualTo(2);
        assertThat(summary.skippedCount()).isZero();
        assertThat(summary.entries())
                .extracting(entry -> entry.status())
                .containsExactly(FirebaseUserImportStatus.WOULD_CREATE, FirebaseUserImportStatus.WOULD_CREATE);
        assertThat(rowCount("app_users")).isZero();
        assertThat(rowCount("user_profiles")).isZero();
    }

    @Test
    void eventDryRunReportsWouldCreateWhenProfilesAndContentsExist() throws Exception {
        Path stagingDir = prepareStagingDir("event-dry-run");
        seedMigratableContents();
        firebaseUserMigrationJob.run(command(false, stagingDir));

        FirebaseMigrationCommand dryRunCommand = command(true, stagingDir);
        FirebaseEventImportSummary contentSummary =
                firebaseEventMigrationJob.runContentEvents(dryRunCommand.contentEventsFile(), dryRunCommand.dryRun());
        FirebaseEventImportSummary appSummary =
                firebaseEventMigrationJob.runAppEvents(dryRunCommand.appEventsFile(), dryRunCommand.dryRun());

        assertThat(contentSummary.dryRun()).isTrue();
        assertThat(contentSummary.processedCount()).isEqualTo(3);
        assertThat(contentSummary.createdCount()).isEqualTo(3);
        assertThat(contentSummary.skippedCount()).isZero();
        assertThat(contentSummary.entries())
                .extracting(entry -> entry.status())
                .containsExactly(
                        FirebaseEventImportStatus.WOULD_CREATE,
                        FirebaseEventImportStatus.WOULD_CREATE,
                        FirebaseEventImportStatus.WOULD_CREATE);
        assertThat(appSummary.dryRun()).isTrue();
        assertThat(appSummary.processedCount()).isEqualTo(3);
        assertThat(appSummary.createdCount()).isEqualTo(3);
        assertThat(appSummary.skippedCount()).isZero();
        assertThat(rowCount("content_events")).isZero();
        assertThat(rowCount("app_events")).isZero();
    }

    @Test
    void migrationJobsImportFixturesAndRemainIdempotentOnRepeatRuns() throws Exception {
        Path stagingDir = prepareStagingDir("happy-path");
        seedMigratableContents();
        FirebaseMigrationCommand command = command(false, stagingDir);

        FirebaseUserImportSummary firstUserImport = firebaseUserMigrationJob.run(command);
        FirebaseEventImportSummary firstContentImport =
                firebaseEventMigrationJob.runContentEvents(command.contentEventsFile(), command.dryRun());
        FirebaseEventImportSummary firstAppImport =
                firebaseEventMigrationJob.runAppEvents(command.appEventsFile(), command.dryRun());

        assertThat(firstUserImport.createdCount()).isEqualTo(2);
        assertThat(firstContentImport.createdCount()).isEqualTo(3);
        assertThat(firstAppImport.createdCount()).isEqualTo(3);
        assertThat(rowCount("app_users")).isEqualTo(2);
        assertThat(rowCount("user_profiles")).isEqualTo(2);
        assertThat(rowCount("content_events")).isEqualTo(3);
        assertThat(rowCount("app_events")).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select display_name from user_profiles where app_user_id = (select id from app_users where firebase_uid = ?) and is_primary = true",
                String.class,
                "firebase-user-1")).isEqualTo("Ada");
        assertThat(jdbcTemplate.queryForObject(
                "select event_type from content_events where legacy_event_key = ?",
                String.class,
                "firebase-content-1")).isEqualTo("START");
        assertThat(jdbcTemplate.queryForObject(
                "select event_type from content_events where legacy_event_key = ?",
                String.class,
                "firebase-content-2")).isEqualTo("EXIT");
        assertThat(jdbcTemplate.queryForObject(
                "select left_page from content_events where legacy_event_key = ?",
                Integer.class,
                "firebase-content-2")).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "select event_type from content_events where legacy_event_key = ?",
                String.class,
                "firebase-content-3")).isEqualTo("COMPLETE");
        assertThat(jdbcTemplate.queryForObject(
                "select event_type from app_events where legacy_event_key = ?",
                String.class,
                "firebase-app-2")).isEqualTo("LOCKED_CONTENT_CLICKED");
        assertThat(jdbcTemplate.queryForList(
                "select content_id from app_events where legacy_event_key = ?",
                "firebase-app-2").getFirst().get("content_id")).isNotNull();
        assertThat(jdbcTemplate.queryForList(
                "select content_id from app_events where legacy_event_key = ?",
                "firebase-app-1").getFirst().get("content_id")).isNull();

        FirebaseUserImportSummary repeatedUserImport = firebaseUserMigrationJob.run(command);
        FirebaseEventImportSummary repeatedContentImport =
                firebaseEventMigrationJob.runContentEvents(command.contentEventsFile(), command.dryRun());
        FirebaseEventImportSummary repeatedAppImport =
                firebaseEventMigrationJob.runAppEvents(command.appEventsFile(), command.dryRun());

        assertThat(repeatedUserImport.createdCount()).isZero();
        assertThat(repeatedUserImport.skippedCount()).isEqualTo(2);
        assertThat(repeatedUserImport.entries())
                .extracting(entry -> entry.status())
                .containsExactly(FirebaseUserImportStatus.SKIPPED_EXISTING, FirebaseUserImportStatus.SKIPPED_EXISTING);
        assertThat(repeatedContentImport.createdCount()).isZero();
        assertThat(repeatedContentImport.skippedCount()).isEqualTo(3);
        assertThat(repeatedContentImport.entries())
                .extracting(entry -> entry.status())
                .containsExactly(
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE);
        assertThat(repeatedAppImport.createdCount()).isZero();
        assertThat(repeatedAppImport.skippedCount()).isEqualTo(3);
        assertThat(repeatedAppImport.entries())
                .extracting(entry -> entry.status())
                .containsExactly(
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                        FirebaseEventImportStatus.SKIPPED_DUPLICATE);
        assertThat(rowCount("app_users")).isEqualTo(2);
        assertThat(rowCount("user_profiles")).isEqualTo(2);
        assertThat(rowCount("content_events")).isEqualTo(3);
        assertThat(rowCount("app_events")).isEqualTo(3);
    }

    private void seedMigratableContents() {
        contentManagementService.createContent(new CreateContentCommand(ContentType.STORY, "moonlight-story", 5, true));
        contentManagementService.createContent(
                new CreateContentCommand(ContentType.MEDITATION, "forest-meditation", 7, true));
    }

    private FirebaseMigrationCommand command(boolean dryRun, Path stagingDir) {
        return new FirebaseMigrationCommand(dryRun, stagingDir, true, true, true,
                "users.json", "content-events.json", "app-events.json");
    }

    private Path prepareStagingDir(String name) throws IOException {
        Path stagingDir = Files.createDirectory(tempDir.resolve(name));
        copyFixture("fixtures/firebase/users.json", stagingDir.resolve("users.json"));
        copyFixture("fixtures/firebase/content-events.json", stagingDir.resolve("content-events.json"));
        copyFixture("fixtures/firebase/app-events.json", stagingDir.resolve("app-events.json"));
        return stagingDir;
    }

    private void copyFixture(String resourcePath, Path target) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private int rowCount(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
    }
}
