package com.tellpal.v2.presentation.dto.asset;

import java.time.OffsetDateTime;

public record AssetProcessingResponse(
        Long contentId,
        String languageCode,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String errorMessage
) {}
