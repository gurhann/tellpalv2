package com.tellpal.v2.user.application;

import com.tellpal.v2.user.domain.AppUser;
import com.tellpal.v2.user.domain.UserProfileRepository;
import com.tellpal.v2.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional
public class FirebaseMigrationService {

    public record ImportContentEventResult(
            String firebaseUid,
            Long contentId,
            String languageCode,
            String eventType,
            String legacyEventKey,
            OffsetDateTime occurredAt
    ) {}

    public record ImportAppEventResult(
            String firebaseUid,
            String eventType,
            String legacyEventKey,
            OffsetDateTime occurredAt
    ) {}

    private final UserApplicationService userApplicationService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public FirebaseMigrationService(UserApplicationService userApplicationService,
                                    UserRepository userRepository,
                                    UserProfileRepository userProfileRepository) {
        this.userApplicationService = userApplicationService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public AppUser importUser(String firebaseUid) {
        return userApplicationService.registerOrGetUser(firebaseUid);
    }

    public String mapEventType(String legacyEventType) {
        if (legacyEventType == null) return null;
        return switch (legacyEventType) {
            case "START_CONTENT"  -> "START";
            case "LEFT_CONTENT"   -> "EXIT";
            case "FINISH_CONTENT" -> "COMPLETE";
            default               -> legacyEventType;
        };
    }

    public ImportContentEventResult importContentEvent(String firebaseUid,
                                                       Long contentId,
                                                       String languageCode,
                                                       String legacyEventType,
                                                       String legacyEventKey,
                                                       OffsetDateTime occurredAt) {
        String mappedEventType = mapEventType(legacyEventType);
        return new ImportContentEventResult(firebaseUid, contentId, languageCode,
                mappedEventType, legacyEventKey, occurredAt);
    }

    public ImportAppEventResult importAppEvent(String firebaseUid,
                                               String legacyEventType,
                                               String legacyEventKey,
                                               OffsetDateTime occurredAt) {
        String mappedEventType = mapEventType(legacyEventType);
        return new ImportAppEventResult(firebaseUid, mappedEventType, legacyEventKey, occurredAt);
    }
}
