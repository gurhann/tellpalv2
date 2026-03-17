package com.tellpal.v2.event.domain;

import java.util.Optional;
import java.util.UUID;

public interface AppEventRepository {

    Optional<AppEvent> findById(UUID eventId);

    Optional<AppEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    boolean existsById(UUID eventId);

    AppEvent save(AppEvent appEvent);
}
