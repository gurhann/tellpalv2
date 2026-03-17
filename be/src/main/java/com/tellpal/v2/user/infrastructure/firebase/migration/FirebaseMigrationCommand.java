package com.tellpal.v2.user.infrastructure.firebase.migration;

import java.nio.file.Path;

public record FirebaseMigrationCommand(
        boolean dryRun,
        Path stagingDir,
        boolean importUsers,
        boolean importContentEvents,
        boolean importAppEvents,
        String usersFileName,
        String contentEventsFileName,
        String appEventsFileName) {

    public FirebaseMigrationCommand {
        if (stagingDir == null) {
            throw new IllegalArgumentException("Staging directory must not be null");
        }
        usersFileName = requireFileName(usersFileName, "Users file name must not be blank");
        contentEventsFileName = requireFileName(contentEventsFileName, "Content events file name must not be blank");
        appEventsFileName = requireFileName(appEventsFileName, "App events file name must not be blank");
    }

    public static FirebaseMigrationCommand from(FirebaseMigrationProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Firebase migration properties must not be null");
        }
        return new FirebaseMigrationCommand(
                properties.isDryRun(),
                Path.of(requirePath(properties.getStagingDir())),
                properties.isImportUsers(),
                properties.isImportContentEvents(),
                properties.isImportAppEvents(),
                properties.getUsersFileName(),
                properties.getContentEventsFileName(),
                properties.getAppEventsFileName());
    }

    public Path usersFile() {
        return stagingDir.resolve(usersFileName);
    }

    public Path contentEventsFile() {
        return stagingDir.resolve(contentEventsFileName);
    }

    public Path appEventsFile() {
        return stagingDir.resolve(appEventsFileName);
    }

    private static String requirePath(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Staging directory must not be blank");
        }
        return value.trim();
    }

    private static String requireFileName(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
