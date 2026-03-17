package com.tellpal.v2.user.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportEntry;
import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportStatus;
import com.tellpal.v2.user.application.UserMigrationResults.FirebaseUserImportSummary;
import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.AppUserRepository;
import com.tellpal.v2.user.domain.UserProfile;
import com.tellpal.v2.user.infrastructure.firebase.migration.LegacyFirebaseUserImportRecord;
import com.tellpal.v2.user.infrastructure.firebase.migration.LegacyFirebaseUserImportRecord.LegacyFirebaseDefaultProfile;

@Service
public class FirebaseUserMigrationService {

    private final AppUserRepository appUserRepository;
    private final UserAssetReferenceValidator userAssetReferenceValidator;

    public FirebaseUserMigrationService(
            AppUserRepository appUserRepository,
            UserAssetReferenceValidator userAssetReferenceValidator) {
        this.appUserRepository = appUserRepository;
        this.userAssetReferenceValidator = userAssetReferenceValidator;
    }

    @Transactional
    public FirebaseUserImportSummary importUsers(List<LegacyFirebaseUserImportRecord> records, boolean dryRun) {
        List<LegacyFirebaseUserImportRecord> safeRecords = records == null ? List.of() : List.copyOf(records);
        List<FirebaseUserImportEntry> entries = new ArrayList<>();
        int createdCount = 0;
        int skippedCount = 0;

        for (LegacyFirebaseUserImportRecord record : safeRecords) {
            FirebaseUserImportEntry entry = importSingle(record, dryRun);
            entries.add(entry);
            if (entry.status() == FirebaseUserImportStatus.CREATED
                    || entry.status() == FirebaseUserImportStatus.WOULD_CREATE) {
                createdCount++;
            } else {
                skippedCount++;
            }
        }

        return new FirebaseUserImportSummary(
                dryRun,
                safeRecords.size(),
                createdCount,
                skippedCount,
                entries);
    }

    private FirebaseUserImportEntry importSingle(LegacyFirebaseUserImportRecord record, boolean dryRun) {
        LegacyFirebaseUserImportRecord requiredRecord = requireRecord(record);
        return appUserRepository.findByFirebaseUid(requiredRecord.firebaseUid())
                .map(existing -> toSkippedEntry(existing, requiredRecord.firebaseUid()))
                .orElseGet(() -> dryRun
                        ? new FirebaseUserImportEntry(
                                requiredRecord.firebaseUid(),
                                FirebaseUserImportStatus.WOULD_CREATE,
                                null,
                                null)
                        : createUser(requiredRecord));
    }

    private FirebaseUserImportEntry createUser(LegacyFirebaseUserImportRecord record) {
        AppUser createdUser = appUserRepository.save(AppUser.create(record.firebaseUid(), record.allowMarketing()));
        AppUser configuredUser = applyDefaultProfile(createdUser, record.defaultProfile());
        configuredUser = appUserRepository.save(configuredUser);
        UserProfile primaryProfile = configuredUser.primaryProfile()
                .orElseThrow(() -> new IllegalStateException("Imported app user must have a primary profile"));
        return new FirebaseUserImportEntry(
                record.firebaseUid(),
                FirebaseUserImportStatus.CREATED,
                configuredUser.getId(),
                primaryProfile.getId());
    }

    private AppUser applyDefaultProfile(AppUser appUser, LegacyFirebaseDefaultProfile defaultProfile) {
        if (defaultProfile == null) {
            return appUser;
        }
        userAssetReferenceValidator.requireImageAsset(defaultProfile.avatarMediaId(), "avatarMediaId");
        UserProfile primaryProfile = appUser.primaryProfile()
                .orElseThrow(() -> new IllegalStateException("Imported app user must have a primary profile"));
        Long profileId = primaryProfile.getId();
        if (profileId == null || profileId <= 0) {
            throw new IllegalStateException("Primary profile must be persisted before import customization");
        }
        appUser.updateProfile(
                profileId,
                defaultProfile.displayName(),
                normalizeAgeRange(defaultProfile.ageRange()),
                defaultProfile.avatarMediaId(),
                defaultProfile.favoriteGenres().toArray(String[]::new),
                defaultProfile.mainPurposes().toArray(String[]::new),
                true);
        return appUser;
    }

    private FirebaseUserImportEntry toSkippedEntry(AppUser appUser, String firebaseUid) {
        UserProfile primaryProfile = appUser.primaryProfile()
                .orElseThrow(() -> new IllegalStateException("Existing app user must have a primary profile"));
        return new FirebaseUserImportEntry(
                firebaseUid,
                FirebaseUserImportStatus.SKIPPED_EXISTING,
                appUser.getId(),
                primaryProfile.getId());
    }

    private static LegacyFirebaseUserImportRecord requireRecord(LegacyFirebaseUserImportRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Firebase user import record must not be null");
        }
        return record;
    }

    private static String normalizeAgeRange(String ageRange) {
        if (ageRange == null || ageRange.isBlank()) {
            return "UNKNOWN";
        }
        return ageRange.trim();
    }
}
