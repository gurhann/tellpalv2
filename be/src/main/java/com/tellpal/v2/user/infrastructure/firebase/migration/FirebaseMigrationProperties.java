package com.tellpal.v2.user.infrastructure.firebase.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tellpal.firebase-migration")
public class FirebaseMigrationProperties {

    private boolean enabled;
    private boolean dryRun = true;
    private String stagingDir = "be/var/firebase-migration";
    private String usersFileName = "users.json";
    private String contentEventsFileName = "content-events.json";
    private String appEventsFileName = "app-events.json";
    private boolean importUsers = true;
    private boolean importContentEvents = true;
    private boolean importAppEvents = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public String getStagingDir() {
        return stagingDir;
    }

    public void setStagingDir(String stagingDir) {
        this.stagingDir = stagingDir;
    }

    public String getUsersFileName() {
        return usersFileName;
    }

    public void setUsersFileName(String usersFileName) {
        this.usersFileName = usersFileName;
    }

    public String getContentEventsFileName() {
        return contentEventsFileName;
    }

    public void setContentEventsFileName(String contentEventsFileName) {
        this.contentEventsFileName = contentEventsFileName;
    }

    public String getAppEventsFileName() {
        return appEventsFileName;
    }

    public void setAppEventsFileName(String appEventsFileName) {
        this.appEventsFileName = appEventsFileName;
    }

    public boolean isImportUsers() {
        return importUsers;
    }

    public void setImportUsers(boolean importUsers) {
        this.importUsers = importUsers;
    }

    public boolean isImportContentEvents() {
        return importContentEvents;
    }

    public void setImportContentEvents(boolean importContentEvents) {
        this.importContentEvents = importContentEvents;
    }

    public boolean isImportAppEvents() {
        return importAppEvents;
    }

    public void setImportAppEvents(boolean importAppEvents) {
        this.importAppEvents = importAppEvents;
    }
}
