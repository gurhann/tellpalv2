package com.tellpal.v2.asset.api;

import java.util.UUID;

/** Explicit internal local/test source authority. Admission never changes existing content bindings. */
public interface ReadingSourceAdmissionApi {
    /** Independently verifies a pinned original and registers a new immutable ORIGINAL_IMAGE identity. */
    Admission admit(long selectedAssetId, ReadingImagePreparationApi.TestProfile profile);
    /** Permanently withdraws eligibility while retaining the original, admitted object and evidence. */
    void withdraw(UUID admissionId);
    record Admission(UUID id, long assetId, long epoch) {
    }
}
