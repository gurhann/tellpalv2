package com.tellpal.v2.asset.api;

import java.util.UUID;

/** Yerel/test kaynak yetkisini doğrular; içerik bağlarını çağıran content akışı sahiplenir. */
public interface ReadingSourceAdmissionApi {
    /** Ham orijinali bağımsız doğrulayıp yeni ORIGINAL_IMAGE kimliği oluşturur; kabul edilmiş kimliği DB kanıtıyla tekrar kullanır. */
    Admission admit(long selectedAssetId, ReadingImagePreparationApi.TestProfile profile);
    /** Aynı kısa transaction'da kaynak kimliği ve epoch'u kilitleyerek koşullu bağlamayı korur. */
    boolean guard(java.util.List<Admission> admissions);
    /** Permanently withdraws eligibility while retaining the original, admitted object and evidence. */
    void withdraw(UUID admissionId);
    record Admission(UUID id, long assetId, long epoch) {
    }
}
