package com.tellpal.v2.asset.reading.p5g;

import java.nio.file.Path;
import java.util.Map;

/** Kaynak/adayı tam decode eden, ağ engelli tek iş child girişidir. */
public final class P5gChild {
    private P5gChild() { }

    /** Altı açık argüman gerekir; byte pin, MIME, RGB ve metadata hataları nonzero exit üretir. */
    public static void main(String[] args) throws Exception {
        P5gProfile.enabled();
        P5gProfile.require(Runtime.getRuntime().maxMemory() <= 256L * 1024 * 1024
                && Runtime.getRuntime().availableProcessors() == 1
                && java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-XX:MaxDirectMemorySize=32m"),
                "CHILD_EFFECTIVE_BUDGET_REFUSED");
        P5gProfile.require(args.length == 6, "CHILD_ARGUMENTS_REQUIRED");
        byte[] source = pinned(Path.of(args[0]), args[1]);
        byte[] candidate = pinned(Path.of(args[3]), args[4]);
        var original = P5gRawDecoder.decode(source, args[2]);
        var result = P5gRawDecoder.decode(candidate, args[5]);
        P5gRawDecoder.same(original, result);
        if (args[2].equals("image/jpeg")) P5gProfile.require(java.util.Arrays.equals(source, candidate), "JPEG_COPY_REQUIRED");
        System.out.println(P5gProfile.JSON.writeValueAsString(Map.of("status", "VERIFIED", "sourceSha256", args[1],
                "candidateSha256", args[4], "rgbBytesCompared", original.rgb().length,
                "rgbSha256", P5gProfile.sha(original.rgb()), "metadata", original.metadata().summary(),
                "sourceReader", original.reader(), "candidateReader", result.reader(),
                "existingP2Admission", P5gExistingAdmission.inspect(candidate),
                "effectiveRuntime", Map.of("maxHeapBytes", Runtime.getRuntime().maxMemory(),
                        "activeProcessors", Runtime.getRuntime().availableProcessors(), "directMemoryMiB", 32))));
    }

    private static byte[] pinned(Path path, String sha) throws Exception {
        byte[] bytes = P5gProfile.read(path, P5gProfile.ENCODED);
        P5gProfile.require(P5gProfile.sha(bytes).equals(sha), "PIN_SHA_REFUSED"); return bytes;
    }
}
