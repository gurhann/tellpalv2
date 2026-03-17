package com.tellpal.v2.content.api;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface ContentFreeAccessApi {

    ResolvedContentFreeAccessSet resolveFreeAccess(LanguageCode languageCode, String requestedAccessKey);

    boolean isContentFree(Long contentId, LanguageCode languageCode, String requestedAccessKey);
}
