package com.tellpal.v2.asset.reading.p5g;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "p5g.local.enabled", matches = "true")
class P5gCorpusTest {
    @Test void all41PinnedFilesPassIndependentChildRereadAndFullPreservation() throws Exception {
        var report = P5gRunner.run(Path.of(System.getProperty("p5g.report")),
                Path.of(System.getProperty("p5g.baseline")), Path.of(System.getProperty("p5g.repo")),
                Path.of(System.getProperty("p5g.output")));
        assertEquals("LOCAL_COMPATIBILITY_COMPLETE", report.get("status"));
        assertEquals(41, report.get("filesVerified"));
        assertEquals(17, report.get("jpegByteCopies")); assertEquals(24, report.get("losslessWebpByteCopies"));
    }
}
