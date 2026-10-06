package com.tellpal.v2.content.api;

import java.util.List;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Exact-language scalar previews in the supplied curated order. */
public interface ContentPreviewQueryApi {
    /** Filters publication/readiness before the caller applies its preview limit. */
    List<ContentPreview> list(List<Long> contentIds, LanguageCode language, boolean subscriber);
}
