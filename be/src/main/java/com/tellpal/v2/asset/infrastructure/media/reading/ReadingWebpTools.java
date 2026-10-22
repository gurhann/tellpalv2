package com.tellpal.v2.asset.infrastructure.media.reading;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Resmi 1.6.0 araçlarının platforma bağlı sabit binary kimliğini her kullanımda sınar. */
public final class ReadingWebpTools {
    public static final String PROPERTY = "tellpal.reading.webp.tool-directory";
    public static final String ENVIRONMENT = "TELLPAL_READING_WEBP_TOOL_DIRECTORY";
    private static final Map<String, List<String>> SHA = Map.of(
            "mac-arm64", List.of("07ba910950253f18ca227d085848ff39be530f6ef22005e778cd48d4850734f2", "f3719d70061a8482607ba11a64e9ecfc12bd33baf11a376c3c5582ffd9bb71c7"),
            "linux-aarch64", List.of("c1cbc3d46a3beb3883359dc9b112d7f5d08ed213a6a5d954f958b6c8baee7fc8", "4af97a7422a8753164b8bd77b1b74613ed1e9fb603becde41d1aeea3a9332b4c"),
            "linux-x86-64", List.of("5ac7ece6bdda1587f3c169ce74ab2734994f9d14e07fc5358208443c1b29edec", "cda01a9dec4504c2d247c85ad3f902e4d9cf3ce0677d4e209d4528b2cb5b1a72"));
    private ReadingWebpTools() { }

    /** Araç dizini açık system property veya environment ile verilmelidir. */
    public static Path directory() throws IOException {
        String configured = System.getProperty(PROPERTY, System.getenv(ENVIRONMENT));
        if (configured == null || configured.isBlank()) throw new IOException("WEBP_TOOL_DIRECTORY_REQUIRED");
        return Path.of(configured).toAbsolutePath().normalize();
    }

    /** Eksik, çalıştırılamayan veya resmi SHA’dan farklı binary dönüşüm öncesinde reddedilir. */
    public static Path binary(String name) throws IOException {
        int index = switch (name) { case "cwebp" -> 0; case "webpmux" -> 1; default -> throw new IOException("WEBP_TOOL_REFUSED"); };
        String os = System.getProperty("os.name"), arch = System.getProperty("os.arch");
        String platform = os.equals("Mac OS X") && arch.equals("aarch64") ? "mac-arm64"
                : os.equals("Linux") && arch.equals("aarch64") ? "linux-aarch64"
                : os.equals("Linux") && (arch.equals("amd64") || arch.equals("x86_64")) ? "linux-x86-64" : "unsupported";
        if (!SHA.containsKey(platform)) throw new IOException("WEBP_PLATFORM_REFUSED");
        Path path = directory().resolve(name);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !Files.isExecutable(path)
                || Files.size(path) > 32L * 1024 * 1024
                || !ReadingPreservation.sha(Files.readAllBytes(path)).equals(SHA.get(platform).get(index)))
            throw new IOException("WEBP_BINARY_IDENTITY_MISMATCH:" + name);
        return path;
    }

    /** Profil hem cwebp hem webpmux binary SHA'sını ve gerçek sürüm yanıtını içerir. */
    public static Map<String, Object> evidence() {
        try {
            var result = new TreeMap<String, Object>();
            for (String name : List.of("cwebp", "webpmux")) {
                Path binary = binary(name);
                Process process = new ProcessBuilder(binary.toString(), "-version").redirectErrorStream(true).start();
                try {
                    if (!process.waitFor(2, TimeUnit.SECONDS)) throw new IOException("WEBP_VERSION_TIMEOUT");
                    String version = new String(process.getInputStream().readNBytes(1024), java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (process.exitValue() != 0 || !version.equals(name.equals("cwebp") ? "1.6.0\nlibsharpyuv: 0.4.2" : "1.6.0")) throw new IOException("WEBP_VERSION_REFUSED");
                    result.put(name, Map.of("sha256", ReadingPreservation.sha(Files.readAllBytes(binary)), "version", version));
                } finally { process.destroyForcibly(); process.waitFor(); }
            }
            return result;
        } catch (IOException e) { throw new IllegalStateException("WEBP_TOOLS_UNAVAILABLE", e); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("WEBP_TOOLS_INTERRUPTED", e); }
    }
}
