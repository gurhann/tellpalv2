package com.tellpal.v2.asset.reading.p5g;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import java.io.IOException;
import java.util.Map;

/** Yeni codec kanıtını mevcut P2 kabulünden ayrı raporlar. */
final class P5gExistingAdmission {
    private P5gExistingAdmission() { }

    static Map<String, Object> inspect(byte[] bytes) {
        var profile = new TestProfile(1712, 2778, P5gProfile.ENCODED, P5gProfile.ENCODED, 5_000_000,
                512L * 1024 * 1024, 256, 32, 320, P5gProfile.ENCODED * 2,
                10_000, 60_000, 1, 1, false, "LOCAL_COMPATIBILITY_ONLY_NO_OUTPUT");
        try {
            var decoded = ReadingImageInspector.decode(bytes, profile);
            try { return Map.of("status", "ACCEPTED"); } finally { decoded.image().flush(); }
        } catch (IOException error) { return Map.of("status", "REJECTED", "diagnostic", error.getMessage()); }
    }
}
