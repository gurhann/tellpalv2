package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ReadingNativeTestToolsTest {
    @Test void requiredMarkerWithoutToolsFailsRatherThanSkipping() {
        assertThatThrownBy(() -> ReadingNativeTestTools.require(key -> null,
                key -> key.equals("TELLPAL_READING_NATIVE_TESTS_REQUIRED") ? "true" : null))
                .isInstanceOf(AssertionError.class).hasMessage("NATIVE_TEST_TOOLS_REQUIRED");
        assertThatThrownBy(() -> ReadingNativeTestTools.require(key -> null, key -> null))
                .isInstanceOf(org.opentest4j.TestAbortedException.class);
        assertThatCode(() -> ReadingNativeTestTools.require(key -> key.equals("tellpal.reading.webp.tool-directory") ? "/tools" : null,
                key -> key.equals("TELLPAL_READING_NATIVE_TESTS_REQUIRED") ? "true" : null)).doesNotThrowAnyException();
    }
}
