package com.tellpal.v2.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingTargetScope;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RetryAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
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

    @Test
    void narrationScheduleDoesNotRequirePageCountButRequiresAudio() {
        var command = new ScheduleAssetProcessingCommand(
                AssetProcessingTarget.localization(42L, LanguageCode.TR),
                AssetProcessingKind.STORY_NARRATION,
                AssetProcessingContentType.STORY,
                "story",
                null,
                7L,
                null);

        assertThat(command.pageCount()).isZero();
        assertThatIllegalArgumentException().isThrownBy(() -> new ScheduleAssetProcessingCommand(
                AssetProcessingTarget.localization(42L, LanguageCode.TR),
                AssetProcessingKind.STORY_NARRATION,
                AssetProcessingContentType.STORY,
                "story",
                null,
                null,
                null));
    }

    @Test
    void narrationRetryRejectsPageCount() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RetryAssetProcessingCommand(
                AssetProcessingTarget.localization(42L, LanguageCode.TR),
                AssetProcessingKind.STORY_NARRATION,
                AssetProcessingContentType.STORY,
                "story",
                null,
                7L,
                1));
    }
}
