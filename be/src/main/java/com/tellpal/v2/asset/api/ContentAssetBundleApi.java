package com.tellpal.v2.asset.api;

import java.util.Optional;

import com.tellpal.v2.shared.domain.LanguageCode;

/**
 * Read model API for resolving delivery assets for a published content localization.
 */
public interface ContentAssetBundleApi {

    /**
     * Returns generated delivery assets when asset processing for the localization has completed.
     */
    Optional<ContentDeliveryAssets> findForLocalization(Long contentId, LanguageCode languageCode);
}
