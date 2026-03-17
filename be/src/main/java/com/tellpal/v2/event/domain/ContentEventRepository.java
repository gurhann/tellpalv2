package com.tellpal.v2.event.domain;

import java.util.Optional;
import java.util.UUID;

public interface ContentEventRepository {

    Optional<ContentEvent> findById(UUID eventId);

    Optional<ContentEvent> findByProfileIdAndLegacyEventKey(Long profileId, String legacyEventKey);

    boolean existsById(UUID eventId);

    ContentEvent save(ContentEvent contentEvent);
}
