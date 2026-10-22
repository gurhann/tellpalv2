package com.tellpal.v2.asset.reading;

import com.tellpal.v2.asset.infrastructure.media.reading.ReadingWebpTools;
import java.util.function.Function;
import org.junit.jupiter.api.Assumptions;

/** CI zorunluluğunda eksik araç test atlaması değil hata üretir. */
public final class ReadingNativeTestTools {
    private ReadingNativeTestTools() { }
    public static void require() { require(System::getProperty, System::getenv); }
    static void require(Function<String,String> property, Function<String,String> environment) {
        String directory = property.apply(ReadingWebpTools.PROPERTY);
        if (directory == null) directory = environment.apply(ReadingWebpTools.ENVIRONMENT);
        boolean available = directory != null && !directory.isBlank();
        boolean required = "true".equals(property.apply("tellpal.reading.native-tests-required"))
                || "true".equals(environment.apply("TELLPAL_READING_NATIVE_TESTS_REQUIRED"));
        if (required && !available) throw new AssertionError("NATIVE_TEST_TOOLS_REQUIRED");
        Assumptions.assumeTrue(available, "Pinli resmi native araç dizini açık verilmelidir");
    }
}
