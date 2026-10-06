package com.tellpal.v2.asset.application;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.media.ListingCoverProfile;
import com.tellpal.v2.asset.infrastructure.storage.AssetStorageClientRegistry;

/** Signs verified thumbnail IDs without mutating cached download URLs in a read transaction. */
@Service
@Transactional(readOnly=true)
public class LibraryCoverQueryService implements LibraryCoverQueryApi {
    private final ListingCoverRevisionStore revisions;
    private final ListingCoverProfile profile;
    private final AssetRegistryApi assets;
    private final AssetStorageClientRegistry storage;
    private final Clock clock;
    public LibraryCoverQueryService(ListingCoverRevisionStore revisions,ListingCoverProfile profile,AssetRegistryApi assets,
            AssetStorageClientRegistry storage,Clock clock) {this.revisions=revisions;this.profile=profile;this.assets=assets;this.storage=storage;this.clock=clock;}
    @Override public Covers find(long contentId,String language,Long source) {
        if(source==null) return Covers.empty();
        return revisions.find(contentId,language,source,profile.version()).map(r->new Covers(resolve(r.phoneAssetId(),AssetKind.THUMBNAIL_PHONE),
                resolve(r.tabletAssetId(),AssetKind.THUMBNAIL_TABLET))).orElse(Covers.empty());
    }
    private Cover resolve(long id,AssetKind expected) {
        var asset=assets.findById(id).orElse(null);
        if(asset==null || asset.kind()!=expected || asset.mediaType()!=AssetMediaType.IMAGE || !"image/jpeg".equals(asset.mimeType())
                || asset.byteSize()==null || asset.byteSize()<=0 || asset.checksumSha256()==null || asset.checksumSha256().length()!=64) return null;
        var signed=storage.createSignedDownloadUrl(StorageProvider.valueOf(asset.storageLocation().provider().name()),asset.storageLocation().objectPath(),Instant.now(clock));
        return new Cover(id,expected.name(),"image/jpeg",signed.url(),signed.expiresAt());
    }
}
