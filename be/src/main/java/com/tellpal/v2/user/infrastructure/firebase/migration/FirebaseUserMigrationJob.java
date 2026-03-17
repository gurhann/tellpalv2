package com.tellpal.v2.user.infrastructure.firebase.migration;

import org.springframework.stereotype.Component;

import com.tellpal.v2.user.application.FirebaseUserMigrationService;
import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportSummary;

@Component
public class FirebaseUserMigrationJob {

    private final FirebaseMigrationStagingReader firebaseMigrationStagingReader;
    private final FirebaseUserMigrationService firebaseUserMigrationService;

    public FirebaseUserMigrationJob(
            FirebaseMigrationStagingReader firebaseMigrationStagingReader,
            FirebaseUserMigrationService firebaseUserMigrationService) {
        this.firebaseMigrationStagingReader = firebaseMigrationStagingReader;
        this.firebaseUserMigrationService = firebaseUserMigrationService;
    }

    public FirebaseUserImportSummary run(FirebaseMigrationCommand command) {
        FirebaseMigrationCommand requiredCommand = requireCommand(command);
        return firebaseUserMigrationService.importUsers(
                firebaseMigrationStagingReader.readList(requiredCommand.usersFile(), LegacyFirebaseUserImportRecord.class),
                requiredCommand.dryRun());
    }

    private static FirebaseMigrationCommand requireCommand(FirebaseMigrationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Firebase migration command must not be null");
        }
        return command;
    }
}
