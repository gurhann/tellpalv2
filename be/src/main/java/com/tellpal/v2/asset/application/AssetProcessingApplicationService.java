package com.tellpal.v2.asset.application;

import com.tellpal.v2.asset.domain.AssetProcessing;
import com.tellpal.v2.asset.domain.AssetProcessingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AssetProcessingApplicationService {

    private final AssetProcessingRepository assetProcessingRepository;
    private final ImageOptimizationService imageOptimizationService;
    private final AudioOptimizationService audioOptimizationService;
    private final ZipPackagingService zipPackagingService;

    public AssetProcessingApplicationService(AssetProcessingRepository assetProcessingRepository,
                                             ImageOptimizationService imageOptimizationService,
                                             AudioOptimizationService audioOptimizationService,
                                             ZipPackagingService zipPackagingService) {
        this.assetProcessingRepository = assetProcessingRepository;
        this.imageOptimizationService = imageOptimizationService;
        this.audioOptimizationService = audioOptimizationService;
        this.zipPackagingService = zipPackagingService;
    }

    public AssetProcessing startProcessing(Long contentId, String languageCode) {
        AssetProcessing assetProcessing = assetProcessingRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseGet(() -> assetProcessingRepository.save(new AssetProcessing(contentId, languageCode)));
        assetProcessing.startProcessing();
        return assetProcessingRepository.save(assetProcessing);
    }

    @Transactional(readOnly = true)
    public AssetProcessing getProcessingStatus(Long contentId, String languageCode) {
        return assetProcessingRepository.findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, languageCode));
    }

    public AssetProcessing retryProcessing(Long contentId, String languageCode) {
        AssetProcessing assetProcessing = assetProcessingRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, languageCode));
        assetProcessing.resetForRetry();
        assetProcessing.startProcessing();
        return assetProcessingRepository.save(assetProcessing);
    }

    public AssetProcessing completeProcessing(Long contentId, String languageCode) {
        AssetProcessing assetProcessing = assetProcessingRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, languageCode));
        assetProcessing.completeProcessing();
        return assetProcessingRepository.save(assetProcessing);
    }

    public AssetProcessing failProcessing(Long contentId, String languageCode, String errorMessage) {
        AssetProcessing assetProcessing = assetProcessingRepository
                .findByContentIdAndLanguageCode(contentId, languageCode)
                .orElseThrow(() -> new AssetProcessingNotFoundException(contentId, languageCode));
        assetProcessing.failProcessing(errorMessage);
        return assetProcessingRepository.save(assetProcessing);
    }

    @Transactional(readOnly = true)
    public List<AssetProcessing> listByContentId(Long contentId) {
        return assetProcessingRepository.findByContentId(contentId);
    }
}
