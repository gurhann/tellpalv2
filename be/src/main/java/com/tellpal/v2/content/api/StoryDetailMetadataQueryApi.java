package com.tellpal.v2.content.api;

import java.util.Optional;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Reads exact-language story metadata without granting protected-media access. */
public interface StoryDetailMetadataQueryApi {
    Optional<StoryDetailMetadata> find(long contentId, LanguageCode language, boolean subscriber);
}
