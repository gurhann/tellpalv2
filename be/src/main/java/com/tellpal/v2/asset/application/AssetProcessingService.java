package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.tellpal.v2.asset.api.AssetProcessingApi;
import com.tellpal.v2.asset.api.AssetProcessingCommands.CompleteAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.FailAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RecoverExpiredAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.RetryAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.ScheduleAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingCommands.StartAssetProcessingCommand;
import com.tellpal.v2.asset.api.AssetProcessingRecord;
import com.tellpal.v2.asset.api.AssetProcessingStatusChangedEvent;
import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;
import com.tellpal.v2.asset.domain.AssetProcessingStatus;
import com.tellpal.v2.asset.domain.ProcessingContentType;
import com.tellpal.v2.shared.domain.LanguageCode;

import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyCompletedException;
import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyPendingException;
import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingAlreadyRunningException;
import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingNotFoundException;
import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingRetryRequiredException;
import static com.tellpal.v2.asset.application.AssetProcessingApplicationExceptions.AssetProcessingLocalizationNotFoundException;

/**
 * Application service that orchestrates the asset processing state machine.
 *
 * <p>The service persists workflow transitions, enforces allowed lifecycle moves, and publishes a
 * status-changed event after each stored transition.
 */
@Service
public class AssetProcessingService implements AssetProcessingApi {

    private static final Duration DEFAULT_LEASE_DURATION = Duration.ofMinutes(10);
    private static final Logger log = LoggerFactory.getLogger(AssetProcessingService.class);

    private final Clock clock;
    private final AssetProcessingRepository assetProcessingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AssetProcessingService(
            Clock clock,
            AssetProcessingRepository assetProcessingRepository,
            ApplicationEventPublisher eventPublisher) {
        this.clock = clock;
        this.assetProcessingRepository = assetProcessingRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Schedules processing for a localization or refreshes the source context of an existing
     * pending entry.
     */
    @Override
    @Transactional
    public AssetProcessingRecord schedule(ScheduleAssetProcessingCommand command) {
        Optional<AssetProcessing> existingProcessing = assetProcessingRepository.findByContentIdAndLanguageCode(
                command.contentId(),
                command.languageCode());
        if (existingProcessing.isPresent()) {
            return handleExistingSchedule(existingProcessing.get(), command);
        }
        AssetProcessing created = AssetProcessing.schedule(
                command.contentId(),
                command.languageCode(),
                ProcessingContentType.valueOf(command.contentType().name()),
                command.externalKey(),
                command.coverSourceAssetId(),
                command.audioSourceAssetId(),
                command.pageCount(),
                Instant.now(clock));
        AssetProcessing saved = saveNewProcessing(created);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Transitions a pending entry into processing and acquires a lease for the worker.
     */
    @Override
    @Transactional
    public AssetProcessingRecord start(StartAssetProcessingCommand command) {
        AssetProcessing assetProcessing = loadProcessing(command.contentId(), command.languageCode());
        ensureStartable(assetProcessing);
        assetProcessing.start(Instant.now(clock), DEFAULT_LEASE_DURATION);
        AssetProcessing saved = assetProcessingRepository.save(assetProcessing);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Re-queues a failed entry after replacing its source processing context.
     */
    @Override
    @Transactional
    public AssetProcessingRecord retry(RetryAssetProcessingCommand command) {
        AssetProcessing assetProcessing = loadProcessing(command.contentId(), command.languageCode());
        ensureRetryable(assetProcessing);
        assetProcessing.refreshContext(
                ProcessingContentType.valueOf(command.contentType().name()),
                command.externalKey(),
                command.coverSourceAssetId(),
                command.audioSourceAssetId(),
                command.pageCount());
        assetProcessing.retry(Instant.now(clock));
        AssetProcessing saved = assetProcessingRepository.save(assetProcessing);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Recovers a stuck processing entry whose lease has expired.
     */
    @Override
    @Transactional
    public AssetProcessingRecord recoverExpiredLease(RecoverExpiredAssetProcessingCommand command) {
        AssetProcessing assetProcessing = loadProcessing(command.contentId(), command.languageCode());
        assetProcessing.recoverExpiredLease(
                Instant.now(clock),
                "LEASE_EXPIRED",
                "Processing lease expired before completion");
        AssetProcessing saved = assetProcessingRepository.save(assetProcessing);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Marks an in-flight processing entry as completed.
     */
    @Override
    @Transactional
    public AssetProcessingRecord complete(CompleteAssetProcessingCommand command) {
        AssetProcessing assetProcessing = loadProcessing(command.contentId(), command.languageCode());
        assetProcessing.complete(Instant.now(clock));
        AssetProcessing saved = assetProcessingRepository.save(assetProcessing);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Marks an in-flight processing entry as failed and stores worker diagnostics.
     */
    @Override
    @Transactional
    public AssetProcessingRecord fail(FailAssetProcessingCommand command) {
        AssetProcessing assetProcessing = loadProcessing(command.contentId(), command.languageCode());
        assetProcessing.fail(command.errorCode(), command.errorMessage(), Instant.now(clock));
        AssetProcessing saved = assetProcessingRepository.save(assetProcessing);
        publishStatusChanged(saved);
        return AssetProcessingMapper.toRecord(saved);
    }

    /**
     * Returns the current processing snapshot for a content localization when it exists.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<AssetProcessingRecord> findByLocalization(Long contentId, LanguageCode languageCode) {
        return assetProcessingRepository.findByContentIdAndLanguageCode(requireContentId(contentId), requireLanguageCode(languageCode))
                .map(AssetProcessingMapper::toRecord);
    }

    /**
     * Returns recent processing entries for operational screens and jobs.
     */
    @Override
    @Transactional(readOnly = true)
    public List<AssetProcessingRecord> listRecent(int limit) {
        return assetProcessingRepository.findRecent(sanitizeLimit(limit)).stream()
                .map(AssetProcessingMapper::toRecord)
                .toList();
    }

    private AssetProcessingRecord handleExistingSchedule(
            AssetProcessing assetProcessing,
            ScheduleAssetProcessingCommand command) {
        AssetProcessingStatus status = assetProcessing.getStatus();
        if (status == AssetProcessingStatus.PENDING) {
            assetProcessing.refreshContext(
                    ProcessingContentType.valueOf(command.contentType().name()),
                    command.externalKey(),
                    command.coverSourceAssetId(),
                    command.audioSourceAssetId(),
                    command.pageCount());
            return AssetProcessingMapper.toRecord(assetProcessingRepository.save(assetProcessing));
        }
        if (status == AssetProcessingStatus.PROCESSING) {
            throw new AssetProcessingAlreadyRunningException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        if (status == AssetProcessingStatus.COMPLETED) {
            throw new AssetProcessingAlreadyCompletedException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        throw new AssetProcessingRetryRequiredException(
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode());
    }

    private AssetProcessing loadProcessing(Long contentId, LanguageCode languageCode) {
        Long requiredContentId = requireContentId(contentId);
        LanguageCode requiredLanguageCode = requireLanguageCode(languageCode);
        return assetProcessingRepository.findByContentIdAndLanguageCode(requiredContentId, requiredLanguageCode)
                .orElseThrow(() -> new AssetProcessingNotFoundException(requiredContentId, requiredLanguageCode));
    }

    private AssetProcessing saveNewProcessing(AssetProcessing assetProcessing) {
        try {
            return assetProcessingRepository.save(assetProcessing);
        } catch (DataIntegrityViolationException exception) {
            throw new AssetProcessingLocalizationNotFoundException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
    }

    private void ensureStartable(AssetProcessing assetProcessing) {
        AssetProcessingStatus status = assetProcessing.getStatus();
        if (status == AssetProcessingStatus.PENDING) {
            return;
        }
        if (status == AssetProcessingStatus.PROCESSING) {
            throw new AssetProcessingAlreadyRunningException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        if (status == AssetProcessingStatus.COMPLETED) {
            throw new AssetProcessingAlreadyCompletedException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        throw new AssetProcessingRetryRequiredException(
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode());
    }

    private void ensureRetryable(AssetProcessing assetProcessing) {
        AssetProcessingStatus status = assetProcessing.getStatus();
        if (status == AssetProcessingStatus.FAILED) {
            return;
        }
        if (status == AssetProcessingStatus.PENDING) {
            throw new AssetProcessingAlreadyPendingException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        if (status == AssetProcessingStatus.PROCESSING) {
            throw new AssetProcessingAlreadyRunningException(
                    assetProcessing.getContentId(),
                    assetProcessing.getLanguageCode());
        }
        throw new AssetProcessingAlreadyCompletedException(
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode());
    }

    private void publishStatusChanged(AssetProcessing assetProcessing) {
        log.info(
                "asset_processing_transition contentId={} languageCode={} status={} contentType={} externalKey={} attemptCount={} errorCode={} leaseExpiresAt={}",
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode().value(),
                assetProcessing.getStatus(),
                assetProcessing.getContentType(),
                assetProcessing.getExternalKey(),
                assetProcessing.getAttemptCount(),
                assetProcessing.getLastErrorCode(),
                assetProcessing.getLeaseExpiresAt());
        eventPublisher.publishEvent(new AssetProcessingStatusChangedEvent(
                assetProcessing.getContentId(),
                assetProcessing.getLanguageCode(),
                com.tellpal.v2.asset.api.AssetProcessingState.valueOf(assetProcessing.getStatus().name())));
    }

    private static Long requireContentId(Long contentId) {
        if (contentId == null || contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive");
        }
        return contentId;
    }

    private static LanguageCode requireLanguageCode(LanguageCode languageCode) {
        if (languageCode == null) {
            throw new IllegalArgumentException("Language code must not be null");
        }
        return languageCode;
    }

    private static int sanitizeLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        return Math.min(limit, 100);
    }
}
