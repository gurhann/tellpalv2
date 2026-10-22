package com.tellpal.v2.asset.reading.p5g;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class P5gGateTest {
    @Test void closedPropertyFailsBeforeCodecResolution() {
        String previous = System.getProperty("p5g.local.enabled");
        try {
            System.clearProperty("p5g.local.enabled");
            assertEquals("P5G_PROFILE_CLOSED", assertThrows(IOException.class, P5gProfile::enabled).getMessage());
            if (previous == null) {
                assertThrows(ClassNotFoundException.class, () -> Class.forName(P5gProfile.SPI));
                System.setProperty("p5g.local.enabled", "true");
                assertEquals("P5G_CODEC_MISSING", assertThrows(IOException.class, P5gProfile::enabled).getMessage());
            }
        } finally {
            if (previous == null) System.clearProperty("p5g.local.enabled");
            else System.setProperty("p5g.local.enabled", previous);
        }
    }

    @Test void exactOperationalBoundariesRejectOnlyAboveLimit() {
        assertNull(P5gSupervisor.violation(30_000, 30_000, P5gProfile.RSS, P5gProfile.RSS, P5gProfile.RESPONSE));
        assertEquals("CHILD_TIMEOUT", P5gSupervisor.violation(30_001, 30_000, 0, P5gProfile.RSS, 0));
        assertEquals("CHILD_RSS_LIMIT", P5gSupervisor.violation(0, 30_000, P5gProfile.RSS + 1, P5gProfile.RSS, 0));
        assertEquals("CHILD_LOG_LIMIT", P5gSupervisor.violation(0, 30_000, 0, P5gProfile.RSS, P5gProfile.RESPONSE + 1));
    }

    @Test void allLocalSettingsAreBoundedAndDoNotAdmitProduction() {
        assertEquals(256, P5gProfile.HEAP_MIB); assertEquals(30_000, P5gProfile.TIMEOUT_MILLIS);
        assertEquals(512L * 1024 * 1024, P5gProfile.RSS);
        assertEquals("P5G_LOCAL_ONLY_NO_P2_P3_ADMISSION", P5gProfile.settings().get("scope"));
    }
}
