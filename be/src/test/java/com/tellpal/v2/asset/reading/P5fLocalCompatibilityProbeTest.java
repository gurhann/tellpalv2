package com.tellpal.v2.asset.reading;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class P5fLocalCompatibilityProbeTest {
    @Test void realInspectorAcceptsSupportedOpaqueRgbPng() throws Exception {
        ImageIO.setUseCache(false);
        var image = new BufferedImage(16, 12, BufferedImage.TYPE_INT_RGB);
        try {
            var output = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(image, "PNG", output));
            var result = P5fLocalCompatibilityProbe.inspect(output.toByteArray());
            assertEquals("ACCEPTED", result.get("status"));
            assertEquals("image/png", result.get("mime"));
            assertEquals(16, result.get("width"));
            assertEquals(12, result.get("height"));
        } finally { image.flush(); }
    }

    @Test void realInspectorRejectsWebpBeforeAnyProviderAdmission() {
        var result = P5fLocalCompatibilityProbe.inspect("RIFF0000WEBPVP8L".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        assertEquals("REJECTED", result.get("status"));
        assertEquals("PNG_OR_JPEG_REQUIRED", result.get("diagnostic"));
    }

    @Test void wrongHashAndLengthStopBeforeInspector() throws Exception {
        byte[] bytes = {1, 2, 3};
        String hash = P5fLocalCompatibilityProbe.sha(bytes);
        assertDoesNotThrow(() -> P5fLocalCompatibilityProbe.verifyContent(bytes, 3, hash));
        assertEquals("SOURCE_BYTES_REFUSED", assertThrows(IOException.class,
                () -> P5fLocalCompatibilityProbe.verifyContent(bytes, 2, hash)).getMessage());
        bytes[0] = 4;
        assertEquals("SOURCE_SHA_REFUSED", assertThrows(IOException.class,
                () -> P5fLocalCompatibilityProbe.verifyContent(bytes, 3, hash)).getMessage());
    }

    @Test void localTestProfileCoversRealImageWithoutClaimingProductionAdmission() {
        var profile = P5fLocalCompatibilityProbe.profile();
        assertTrue(profile.maxPixels() >= 1712L * 2778);
        assertEquals(1, profile.concurrency());
        assertFalse(profile.allowGrowth());
        assertEquals("LOCAL_COMPATIBILITY_ONLY_NO_OUTPUT", profile.growthEvidence());
    }
}
