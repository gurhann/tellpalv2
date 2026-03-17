package com.tellpal.v2.event.api;

import java.time.Instant;
import java.util.List;

public interface EventAttributionApi {

    List<AppEventAttributionCandidate> findAttributionCandidates(
            Long profileId,
            Instant occurredAfterInclusive,
            Instant occurredBeforeInclusive);
}
