package com.tellpal.v2.asset.api;

import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

public interface ContentAssetBundleApi {

    Optional<ContentDeliveryAssets> findForLocalization(Long contentId, LanguageCode languageCode);
}
