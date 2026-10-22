package com.tellpal.v2.asset.reading.p5g;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** P5g'nin üretimden bağımsız, kapalı varsayılanlı deney kimliğidir. */
public final class P5gProfile {
    static final ObjectMapper JSON = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    static final String REPORT_SHA = "960d9945c87598bea04144a493d118a16df8e29f7ce85745c87e8d08ceefa000";
    static final String ICC_SHA = "20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab";
    static final String SPI = "com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi";
    static final int WIDTH = 1712, HEIGHT = 2778, METADATA = 65_536, RESPONSE = 1_048_576;
    static final long ENCODED = 16L * 1024 * 1024, PIXELS = 5_000_000;
    static final long OUTPUT = 256L * 1024 * 1024, RSS = 512L * 1024 * 1024;
    static final int HEAP_MIB = 256, TIMEOUT_MILLIS = 30_000;

    private P5gProfile() { }

    static void enabled() throws IOException {
        require(Boolean.getBoolean("p5g.local.enabled"), "P5G_PROFILE_CLOSED");
        try { Class.forName(SPI); } catch (ClassNotFoundException e) { throw new IOException("P5G_CODEC_MISSING", e); }
    }

    static Map<String, Object> settings() {
        return Map.ofEntries(Map.entry("scope", "P5G_LOCAL_ONLY_NO_P2_P3_ADMISSION"),
                Map.entry("width", WIDTH), Map.entry("height", HEIGHT), Map.entry("encodedLimit", ENCODED),
                Map.entry("pixelLimit", PIXELS), Map.entry("metadataLimit", METADATA), Map.entry("heapMiB", HEAP_MIB),
                Map.entry("rssLimit", RSS), Map.entry("timeoutMillis", TIMEOUT_MILLIS), Map.entry("concurrency", 1), Map.entry("activeProcessorCount", 1), Map.entry("directMemoryMiB", 32),
                Map.entry("outputLimit", OUTPUT), Map.entry("responseAndLogLimit", RESPONSE),
                Map.entry("decoder", "imageio-webp:3.15.3/rawImageType/logical-rgb-bands"),
                Map.entry("webpConsumption", "SAME_CODEC_VP8L_CEIL_CONSUMED_BITS"),
                Map.entry("encoding", "NONE_BYTE_COPY"), Map.entry("exif", "STRIP_EXIF_PREFIX_ONLY"),
                Map.entry("rssCounter", "MACOS_PROC_PID_RUSAGE_V0_RESIDENT_SIZE"),
                Map.entry("rssCounterAbi", "96_BYTES_RSS_UINT64_OFFSET_64"),
                Map.entry("reread", "INDEPENDENT_SUPERVISED_CHILD"), Map.entry("network", "SANDBOX_DENY_NETWORK"));
    }

    static byte[] read(Path path, long max) throws IOException {
        require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "REGULAR_FILE_REQUIRED");
        require(path.toAbsolutePath().normalize().equals(path.toRealPath()), "PATH_ALIAS_REFUSED");
        require(Files.size(path) <= max, "FILE_BYTE_LIMIT");
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(Math.toIntExact(max + 1));
            require(bytes.length <= max, "FILE_BYTE_LIMIT");
            return bytes;
        }
    }

    static String sha(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    static String fileSha(Path path) throws IOException {
        require(Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS), "REGULAR_FILE_REQUIRED");
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[65_536]; int n;
                while ((n = input.read(buffer)) != -1) digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    static void require(boolean condition, String code) throws IOException {
        if (!condition) throw new IOException(code);
    }
}
