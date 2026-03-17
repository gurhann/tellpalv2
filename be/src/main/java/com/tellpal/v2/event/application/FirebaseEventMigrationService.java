package com.tellpal.v2.event.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.content.api.ContentLookupApi;
import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportEntry;
import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportStatus;
import com.tellpal.v2.event.application.EventMigrationResults.FirebaseEventImportSummary;
import com.tellpal.v2.event.domain.AppEvent;
import com.tellpal.v2.event.domain.AppEventRepository;
import com.tellpal.v2.event.domain.AppEventType;
import com.tellpal.v2.event.domain.ContentEvent;
import com.tellpal.v2.event.domain.ContentEventRepository;
import com.tellpal.v2.event.domain.ContentEventType;
import com.tellpal.v2.event.infrastructure.firebase.migration.LegacyFirebaseAppEventImportRecord;
import com.tellpal.v2.event.infrastructure.firebase.migration.LegacyFirebaseContentEventImportRecord;
import com.tellpal.v2.user.api.AppUserProfileReference;
import com.tellpal.v2.user.api.UserLookupApi;

@Service
public class FirebaseEventMigrationService {

    private final ContentEventRepository contentEventRepository;
    private final AppEventRepository appEventRepository;
    private final UserLookupApi userLookupApi;
    private final ContentLookupApi contentLookupApi;
    private final Clock clock;

    public FirebaseEventMigrationService(
            ContentEventRepository contentEventRepository,
            AppEventRepository appEventRepository,
            UserLookupApi userLookupApi,
            ContentLookupApi contentLookupApi,
            Clock clock) {
        this.contentEventRepository = contentEventRepository;
        this.appEventRepository = appEventRepository;
        this.userLookupApi = userLookupApi;
        this.contentLookupApi = contentLookupApi;
        this.clock = clock;
    }

    @Transactional
    public FirebaseEventImportSummary importContentEvents(
            List<LegacyFirebaseContentEventImportRecord> records,
            boolean dryRun) {
        List<LegacyFirebaseContentEventImportRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
        List<FirebaseEventImportEntry> entries = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;

        for (LegacyFirebaseContentEventImportRecord record : safeRecords) {
            FirebaseEventImportEntry entry = importContentEvent(record, dryRun);
            entries.add(entry);
            if (entry.status() == FirebaseEventImportStatus.CREATED
                    || entry.status() == FirebaseEventImportStatus.WOULD_CREATE) {
                createdCount++;
            } else {
                skippedCount++;
            }
        }

        return new FirebaseEventImportSummary(dryRun, safeRecords.size(), createdCount, skippedCount, entries);
    }

    @Transactional
    public FirebaseEventImportSummary importAppEvents(List<LegacyFirebaseAppEventImportRecord> records, boolean dryRun) {
        List<LegacyFirebaseAppEventImportRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
        List<FirebaseEventImportEntry> entries = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;

        for (LegacyFirebaseAppEventImportRecord record : safeRecords) {
            FirebaseEventImportEntry entry = importAppEvent(record, dryRun);
            entries.add(entry);
            if (entry.status() == FirebaseEventImportStatus.CREATED
                    || entry.status() == FirebaseEventImportStatus.WOULD_CREATE) {
                createdCount++;
            } else {
                skippedCount++;
            }
        }

        return new FirebaseEventImportSummary(dryRun, safeRecords.size(), createdCount, skippedCount, entries);
    }

    private FirebaseEventImportEntry importContentEvent(LegacyFirebaseContentEventImportRecord record, boolean dryRun) {
        LegacyFirebaseContentEventImportRecord requiredRecord = requireRecord(record);
        UUID eventId = deterministicEventId("content", requiredRecord.firebaseUid(), requiredRecord.legacyEventKey());
        Optional<AppUserProfileReference> profile = userLookupApi.findPrimaryProfileByFirebaseUid(requiredRecord.firebaseUid());
        if (profile.isEmpty()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_MISSING_PROFILE,
                    eventId);
        }
        Long profileId = profile.get().profileId();
        if (contentEventRepository.findByProfileIdAndLegacyEventKey(profileId, requiredRecord.legacyEventKey()).isPresent()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                    eventId);
        }
        Optional<Long> contentId = contentLookupApi.findByExternalKey(requiredRecord.contentExternalKey())
                .map(com.tellpal.v2.content.api.ContentReference::contentId);
        if (contentId.isEmpty()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_MISSING_CONTENT,
                    eventId);
        }
        if (dryRun) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.WOULD_CREATE,
                    eventId);
        }
        contentEventRepository.save(ContentEvent.record(
                eventId,
                profileId,
                contentId.get(),
                requiredRecord.languageCode(),
                mapContentEventType(requiredRecord.firebaseEventType()),
                requiredRecord.occurredAt(),
                Instant.now(clock),
                requiredRecord.sessionId(),
                requiredRecord.leftPage(),
                requiredRecord.engagementSeconds(),
                requiredRecord.metadata(),
                requiredRecord.legacyEventKey()));
        return new FirebaseEventImportEntry(requiredRecord.legacyEventKey(), FirebaseEventImportStatus.CREATED, eventId);
    }

    private FirebaseEventImportEntry importAppEvent(LegacyFirebaseAppEventImportRecord record, boolean dryRun) {
        LegacyFirebaseAppEventImportRecord requiredRecord = requireRecord(record);
        UUID eventId = deterministicEventId("app", requiredRecord.firebaseUid(), requiredRecord.legacyEventKey());
        Optional<AppUserProfileReference> profile = userLookupApi.findPrimaryProfileByFirebaseUid(requiredRecord.firebaseUid());
        if (profile.isEmpty()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_MISSING_PROFILE,
                    eventId);
        }
        Long profileId = profile.get().profileId();
        if (appEventRepository.findByProfileIdAndLegacyEventKey(profileId, requiredRecord.legacyEventKey()).isPresent()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_DUPLICATE,
                    eventId);
        }
        AppEventType eventType = requiredRecord.eventType();
        Optional<Long> contentId = resolveOptionalContentId(requiredRecord.contentExternalKey());
        if (eventType == AppEventType.LOCKED_CONTENT_CLICKED && contentId.isEmpty()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_MISSING_CONTENT,
                    eventId);
        }
        if (requiredRecord.contentExternalKey() != null && contentId.isEmpty()) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.SKIPPED_MISSING_CONTENT,
                    eventId);
        }
        if (dryRun) {
            return new FirebaseEventImportEntry(
                    requiredRecord.legacyEventKey(),
                    FirebaseEventImportStatus.WOULD_CREATE,
                    eventId);
        }
        appEventRepository.save(AppEvent.record(
                eventId,
                profileId,
                eventType,
                contentId.orElse(null),
                requiredRecord.occurredAt(),
                Instant.now(clock),
                requiredRecord.payload(),
                requiredRecord.legacyEventKey()));
        return new FirebaseEventImportEntry(requiredRecord.legacyEventKey(), FirebaseEventImportStatus.CREATED, eventId);
    }

    private Optional<Long> resolveOptionalContentId(String contentExternalKey) {
        if (contentExternalKey == null || contentExternalKey.isBlank()) {
            return Optional.empty();
        }
        return contentLookupApi.findByExternalKey(contentExternalKey.trim())
                .map(com.tellpal.v2.content.api.ContentReference::contentId);
    }

    private static ContentEventType mapContentEventType(LegacyFirebaseContentEventImportRecord.FirebaseContentEventType type) {
        return switch (type) {
            case START_CONTENT -> ContentEventType.START;
            case LEFT_CONTENT -> ContentEventType.EXIT;
            case FINISH_CONTENT -> ContentEventType.COMPLETE;
        };
    }

    private static UUID deterministicEventId(String stream, String firebaseUid, String legacyEventKey) {
        String source = "%s:%s:%s".formatted(stream, firebaseUid, legacyEventKey);
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private static LegacyFirebaseContentEventImportRecord requireRecord(LegacyFirebaseContentEventImportRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Firebase content event import record must not be null");
        }
        return record;
    }

    private static LegacyFirebaseAppEventImportRecord requireRecord(LegacyFirebaseAppEventImportRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Firebase app event import record must not be null");
        }
        return record;
    }
}
