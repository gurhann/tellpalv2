package com.tellpal.v2.content.application.query;

import java.util.List;

/** Scalar read port that excludes full content, page and audio graphs. */
public interface ContentPreviewReadRepository {
    record Row(long id,String externalKey,String title,Integer ageRange,Integer durationMinutes,Long sourceAssetId) {}
    List<Row> findEligible(List<Long> ids,String language);
}
