package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.AssetProcessingCommands.*;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProfile;

/** Schedules real image work and publishes a verified manifest with completion in one guarded transaction. */
@Service
public class LibraryCoverPreparationService implements LibraryCoverPreparationApi {
    private final AssetProcessingService processing;
    private final AssetProcessingRepository jobs;
    private final ListingCoverRevisionStore revisions;
    private final ListingCoverProfile profile;
    private final Clock clock;
    public LibraryCoverPreparationService(AssetProcessingService processing,AssetProcessingRepository jobs,
            ListingCoverRevisionStore revisions,ListingCoverProfile profile,Clock clock) {
        this.processing=processing;this.jobs=jobs;this.revisions=revisions;this.profile=profile;this.clock=clock;
    }
    @Override @Transactional
    public Preparation prepare(AssetProcessingTarget target,String externalKey,long sourceAssetId) {
        if(!target.isLocalization() || sourceAssetId<=0) throw new IllegalArgumentException("Localized cover source is required");
        var ready=revisions.find(target.contentId(),target.languageCode().value(),sourceAssetId,profile.version());
        if(ready.isPresent()) return new Preparation(null,ready.get().revision(),true);
        var job=processing.scheduleListingCover(new ScheduleAssetProcessingCommand(target,AssetProcessingKind.LISTING_COVER,
                AssetProcessingContentType.STORY,externalKey,sourceAssetId,null,0),profile.version());
        return new Preparation(job.processingId(),null,false);
    }
    @Transactional
    public void publish(AssetProcessingRecord captured,ListingCoverRevisionStore.Revision revision) {
        var job=jobs.findByTargetAndKindForUpdate(captured.target(),AssetProcessingKind.LISTING_COVER).orElseThrow(StaleListingCoverAttemptException::new);
        job.requireListingOwnership(ListingCoverAttempt.captured(captured),Instant.now(clock));
        if(revision.sourceAssetId()!=captured.coverSourceAssetId() || !revision.profile().equals(captured.listingProfileVersion())
                || revision.contentId()!=captured.contentId() || !revision.language().equals(captured.languageCode().value()))
            throw new IllegalArgumentException("Ready manifest differs from captured source/profile");
        revisions.save(revision);
        processing.complete(new CompleteAssetProcessingCommand(captured.target(),captured.kind(),ListingCoverAttempt.captured(captured)));
    }
}
