package com.tellpal.v2.content.api;

import com.tellpal.v2.asset.api.ReadingImageDeliveryApi;

/** Yerel/test normal tüketicinin ücretsiz retained LIVE görsel sınırı. */
public interface ReadingImageConsumerApi {
    /** Auth çağrısından önce açık kapsamı sınar ve çalışma kontrol bütçesini ayırır. */
    void requireConfigured(long contentId, String language);
    /** Ücretsiz, exact-language ve retained LIVE için tam descriptor yakalar. */
    ReadingImageComparisonApi.Admission manifest(long contentId, String language);
    /** Mevcut LIVE kimliği ve ücretsiz erişim değişirse reddeder; lease yenilemez. */
    ReadingImageComparisonApi.Admission status(ReadingImageComparisonApi.Identity identity);
    /** Kaynak kolu yoktur; MIME doğrulanmış descriptor'dan gelir. */
    Payload read(ReadingImageComparisonApi.Identity identity, ReadingImageDeliveryApi.Selection selection);
    record Payload(byte[] bytes, String mime) { }
}
