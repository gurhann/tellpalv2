package com.tellpal.v2.event.infrastructure.firebase.migration;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportSummary;
import com.tellpal.v2.event.application.FirebaseEventMigrationService;

@Component
public class FirebaseEventMigrationJob {

    private final FirebaseEventMigrationStagingReader firebaseEventMigrationStagingReader;
    private final FirebaseEventMigrationService firebaseEventMigrationService;

    public FirebaseEventMigrationJob(
            FirebaseEventMigrationStagingReader firebaseEventMigrationStagingReader,
            FirebaseEventMigrationService firebaseEventMigrationService) {
        this.firebaseEventMigrationStagingReader = firebaseEventMigrationStagingReader;
        this.firebaseEventMigrationService = firebaseEventMigrationService;
    }

    public FirebaseEventImportSummary runContentEvents(Path contentEventsFile, boolean dryRun) {
        return firebaseEventMigrationService.importContentEvents(
                firebaseEventMigrationStagingReader.readList(contentEventsFile, LegacyFirebaseContentEventImportRecord.class),
                dryRun);
    }

    public FirebaseEventImportSummary runAppEvents(Path appEventsFile, boolean dryRun) {
        return firebaseEventMigrationService.importAppEvents(
                firebaseEventMigrationStagingReader.readList(appEventsFile, LegacyFirebaseAppEventImportRecord.class),
                dryRun);
    }
}
