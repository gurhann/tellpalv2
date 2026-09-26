package com.tellpal.v2.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.tellpal.v2.asset.api.AssetProcessingContentType;
import com.tellpal.v2.asset.api.AssetProcessingKind;
import com.tellpal.v2.asset.api.AssetProcessingPageSource;
import com.tellpal.v2.asset.api.AssetProcessingTarget;
import com.tellpal.v2.asset.api.AssetProcessingCommands.CompleteAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;
import com.tellpal.v2.asset.domain.ProcessingContentType;
import com.tellpal.v2.shared.domain.LanguageCode;

class AssetProcessingFollowupSnapshotTest {

    @Test
    void pageMutationDuringProcessingQueuesLatestSnapshotAndDoesNotRejectSchedule() throws Exception {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        AssetProcessingTarget target = AssetProcessingTarget.localization(50L, LanguageCode.TR);
        List<AssetProcessingPageSource> active = List.of(new AssetProcessingPageSource(501L, 1, 1001L, null));
        List<AssetProcessingPageSource> latest = List.of(new AssetProcessingPageSource(501L, 1, 1002L, null));
        AssetProcessing processing = AssetProcessing.schedule(target, AssetProcessingKind.STORY_PAGE_MEDIA,
                ProcessingContentType.STORY, "followup-story", null, null, 1, active, now);
        markPersisted(processing, now);
        processing.start(now, Duration.ofMinutes(10));
        AssetProcessingRepository repository = mock(AssetProcessingRepository.class);
        when(repository.findByTargetAndKind(target, AssetProcessingKind.STORY_PAGE_MEDIA))
                .thenReturn(Optional.of(processing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AssetProcessingService service = new AssetProcessingService(clock, repository, mock(ApplicationEventPublisher.class));

        var scheduled = service.schedule(new ScheduleAssetProcessingCommand(target, AssetProcessingKind.STORY_PAGE_MEDIA,
                AssetProcessingContentType.STORY, "followup-story", null, null, 1, latest));

        assertThat(scheduled.status().name()).isEqualTo("PROCESSING");
        assertThat(processing.getPageSourceFollowup()).isEqualTo(latest);

        var afterCompletion = service.complete(new CompleteAssetProcessingCommand(target, AssetProcessingKind.STORY_PAGE_MEDIA));

        assertThat(afterCompletion.status().name()).isEqualTo("PENDING");
        assertThat(afterCompletion.pageSources()).isEqualTo(latest);
        assertThat(processing.getPageSourceFollowup()).isEmpty();
    }

    private static void markPersisted(AssetProcessing processing, Instant at) throws Exception {
        for (var field : com.tellpal.v2.shared.infrastructure.persistence.BaseJpaEntity.class.getDeclaredFields()) {
            field.setAccessible(true);
            switch (field.getName()) {
                case "id" -> field.set(processing, 1L);
                case "createdAt", "updatedAt" -> field.set(processing, at);
                default -> { }
            }
        }
    }
}
