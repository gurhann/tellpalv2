package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Deney ayarlarını gerçek class, JAR ve JVM byte kimliğine bağlar. */
final class P5gIdentity {
    private P5gIdentity() { }

    static Map<String, Object> capture() throws Exception {
        var result = new LinkedHashMap<String, Object>();
        result.put("settings", P5gProfile.settings());
        var classes = new TreeMap<String, String>(); var jars = new TreeMap<String, String>();
        for (String name : List.of(P5gProfile.class.getName(), P5gMetadata.class.getName(), P5gContainers.class.getName(),
                P5gPng.class.getName(), P5gWebp.class.getName(), P5gRawDecoder.class.getName(),
                P5gRawDecoder.Decoded.class.getName(), P5gIdentity.class.getName(), P5gSupervisor.class.getName(),
                P5gRss.class.getName(),
                P5gChild.class.getName(), P5gRunner.class.getName(), P5gCorpus.class.getName(),
                P5gExistingAdmission.class.getName(),
                "com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector",
                "com.tellpal.v2.asset.api.ReadingImagePreparationApi$TestProfile",
                P5gProfile.SPI, "com.twelvemonkeys.imageio.plugins.webp.WebPImageReader",
                "com.twelvemonkeys.imageio.plugins.webp.lossless.VP8LDecoder",
                "com.twelvemonkeys.imageio.plugins.webp.LSBBitReader")) {
            Class<?> type = Class.forName(name);
            captureClass(type, classes);
        }
        for (String entry : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            Path path = Path.of(entry).toAbsolutePath().normalize();
            if (entry.endsWith(".jar")) jars.put(path.toString(), P5gProfile.fileSha(path));
        }
        var runtime = new TreeMap<String, String>();
        for (String key : List.of("java.runtime.version", "java.vendor", "java.vm.name", "os.name", "os.arch", "os.version"))
            runtime.put(key, System.getProperty(key));
        Path home = Path.of(System.getProperty("java.home"));
        runtime.put("javaSha256", P5gProfile.fileSha(home.resolve("bin/java")));
        runtime.put("modulesSha256", P5gProfile.fileSha(home.resolve("lib/modules")));
        runtime.put("macSystemVersionSha256", P5gProfile.fileSha(Path.of("/System/Library/CoreServices/SystemVersion.plist")));
        runtime.put("sandboxSha256", P5gProfile.fileSha(Path.of("/usr/bin/sandbox-exec")));
        result.put("classSha256", classes); result.put("jarSha256", jars); result.put("runtime", runtime);
        return result;
    }

    private static void captureClass(Class<?> type, Map<String, String> classes) throws IOException {
        try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            P5gProfile.require(input != null, "CLASS_IDENTITY_MISSING");
            classes.put(type.getName(), P5gProfile.sha(input.readAllBytes()));
        }
        for (Class<?> nested : type.getDeclaredClasses()) captureClass(nested, classes);
    }

    static String fingerprint(Map<String, Object> identity, String sourceSha, String candidateSha) throws IOException {
        return P5gProfile.sha(P5gProfile.JSON.writeValueAsBytes(Map.of("identity", identity,
                "sourceSha256", sourceSha, "candidateSha256", candidateSha)));
    }
}
