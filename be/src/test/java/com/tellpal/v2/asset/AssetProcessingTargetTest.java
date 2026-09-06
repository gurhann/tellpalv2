package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingTargetScope;
import com.tellpal.v2.shared.domain.LanguageCode;

class AssetProcessingTargetTest {

    @Test
    void localizationTargetRequiresLanguage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetProcessingTarget(
                        AssetProcessingTargetScope.LOCALIZATION, 42L, null));
    }

    @Test
    void contentTargetRejectsLanguage() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AssetProcessingTarget(
                        AssetProcessingTargetScope.CONTENT, 42L, LanguageCode.TR));
    }
}
