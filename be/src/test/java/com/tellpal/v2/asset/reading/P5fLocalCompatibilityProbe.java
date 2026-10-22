package com.tellpal.v2.asset.reading;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import com.tellpal.v2.shared.domain.LanguageCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/** P5c kopyalarını gerçek inspector ile salt okur; Spring, storage veya yayın başlatmaz. */
public final class P5fLocalCompatibilityProbe {
    static final String REPORT_SHA = "960d9945c87598bea04144a493d118a16df8e29f7ce85745c87e8d08ceefa000";
    static final long ENCODED_LIMIT = 16L * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> SOURCES = List.of(
            "be/src/main/java/com/tellpal/v2/shared/domain/LanguageCode.java",
            "be/src/main/java/com/tellpal/v2/asset/api/ReadingImagePreparationApi.java",
            "be/src/main/java/com/tellpal/v2/asset/infrastructure/media/reading/ReadingImageInspector.java",
            "be/src/test/java/com/tellpal/v2/asset/reading/P5fLocalCompatibilityProbe.java");

    private P5fLocalCompatibilityProbe() { }

    /** İki argüman pin manifesti ve P5c köküdür; yalnız JSON sonuç stdout'a yazılır. */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("MANIFEST_AND_PINNED_ROOT_REQUIRED");
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(run(Path.of(args[0]), Path.of(args[1]))));
    }

    static TestProfile profile() {
        return new TestProfile(1712, 2778, ENCODED_LIMIT, ENCODED_LIMIT, 5_000_000,
                512L * 1024 * 1024, 256, 32, 320, ENCODED_LIMIT * 2,
                10_000, 60_000, 1, 1, false, "LOCAL_COMPATIBILITY_ONLY_NO_OUTPUT");
    }

    static Map<String, Object> run(Path manifestPath, Path rootPath) throws Exception {
        Path root = rootPath.toAbsolutePath().normalize();
        if (!root.equals(root.toRealPath())) throw new IOException("PINNED_ROOT_ALIAS_REFUSED");
        byte[] reportBytes = boundedRead(root.resolve("report.json"), 2 * 1024 * 1024);
        if (!sha(reportBytes).equals(REPORT_SHA)) throw new IOException("REPORT_SHA_REFUSED");
        var expected = expectedFiles(JSON.readTree(reportBytes));
        byte[] manifestBytes = boundedRead(manifestPath, 256 * 1024);
        JsonNode files = JSON.readTree(manifestBytes).path("files");
        if (!files.isArray() || files.size() != 123 || expected.size() != 123) throw new IOException("FILE_COUNT_REFUSED");
        var seen = new java.util.HashSet<String>();
        var results = new ArrayList<Map<String, Object>>();
        for (JsonNode row : files) {
            Path path = Path.of(row.path("path").asText()).toAbsolutePath().normalize();
            String name = path.getFileName().toString();
            Expected pin = expected.get(name);
            if (!root.equals(path.getParent()) || !seen.add(name) || pin == null
                    || row.path("assetId").asLong() != pin.assetId()
                    || !row.path("variant").asText().equals(pin.variant())
                    || row.path("bytes").asLong() != pin.bytes()
                    || !row.path("sha256").asText().equals(pin.sha256())) throw new IOException("MANIFEST_PIN_REFUSED");
            byte[] bytes = boundedRead(path, ENCODED_LIMIT);
            verifyContent(bytes, pin.bytes(), pin.sha256());
            var result = new LinkedHashMap<String, Object>();
            result.put("assetId", pin.assetId()); result.put("variant", pin.variant());
            result.put("file", name); result.put("bytes", bytes.length); result.put("sha256", pin.sha256());
            result.putAll(inspect(bytes)); results.add(result);
        }
        long accepted = results.stream().filter(r -> r.get("status").equals("ACCEPTED")).count();
        var output = new LinkedHashMap<String, Object>();
        output.put("scope", "P5F_LOCAL_INSPECTOR_COMPATIBILITY"); output.put("status", "CHECK_COMPLETE");
        output.put("productionAdmission", "NOT_ADMITTED"); output.put("productionImageExecution", "NOT_RUN");
        output.put("p5cReportSha256", REPORT_SHA); output.put("manifestSha256", sha(manifestBytes));
        output.put("filesChecked", results.size()); output.put("acceptedFiles", accepted);
        output.put("rejectedFiles", results.size() - accepted); output.put("rows", results);
        output.put("localTestProfile", JSON.valueToTree(profile()));
        output.put("sourceSha256", sourceHashes()); output.put("classSha256", classHashes());
        output.put("runtime", runtime()); output.put("imageIoReaders", readers());
        return output;
    }

    record Expected(long assetId, String variant, long bytes, String sha256) { }

    private static Map<String, Expected> expectedFiles(JsonNode report) throws IOException {
        var result = new TreeMap<String, Expected>();
        for (JsonNode source : report.path("sources")) {
            long id = source.path("assetId").asLong();
            result.put(source.path("sourceFile").asText(), new Expected(id, "original",
                    source.path("sourceBytes").asLong(), source.path("sourceSha256").asText()));
            for (String format : List.of("PNG", "WEBP")) {
                JsonNode candidate = source.path("candidates").path(format);
                result.put(candidate.path("file").asText(), new Expected(id, format,
                        candidate.path("bytes").asLong(), candidate.path("sha256").asText()));
            }
        }
        return result;
    }

    static void verifyContent(byte[] bytes, long expectedBytes, String expectedSha) throws Exception {
        if (bytes.length != expectedBytes) throw new IOException("SOURCE_BYTES_REFUSED");
        if (!sha(bytes).equals(expectedSha)) throw new IOException("SOURCE_SHA_REFUSED");
    }

    static Map<String, Object> inspect(byte[] bytes) {
        try {
            var decoded = ReadingImageInspector.decode(bytes, profile());
            try { return Map.of("status", "ACCEPTED", "mime", decoded.mime(),
                    "width", decoded.image().getWidth(), "height", decoded.image().getHeight()); }
            finally { decoded.image().flush(); }
        } catch (IOException e) { return Map.of("status", "REJECTED", "diagnostic", e.getMessage()); }
    }

    private static byte[] boundedRead(Path path, long maximum) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.size(path) > maximum)
            throw new IOException("REGULAR_BOUNDED_FILE_REQUIRED");
        try (var input = Files.newInputStream(path)) {
            byte[] bytes = input.readNBytes(Math.toIntExact(maximum + 1));
            if (bytes.length > maximum) throw new IOException("FILE_BYTE_LIMIT");
            return bytes;
        }
    }

    private static Map<String, String> sourceHashes() throws Exception {
        Path repo = Path.of(System.getProperty("p5f.repo", ".")).toRealPath();
        var result = new TreeMap<String, String>();
        for (String source : SOURCES) result.put(source, hashFile(repo.resolve(source)));
        return result;
    }

    private static Map<String, String> classHashes() throws Exception {
        var result = new TreeMap<String, String>();
        for (Class<?> type : List.of(P5fLocalCompatibilityProbe.class, ReadingImageInspector.class,
                ReadingImageInspector.Decoded.class, TestProfile.class, LanguageCode.class)) {
            try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                if (input == null) throw new IOException("CLASS_IDENTITY_MISSING");
                result.put(type.getName(), sha(input.readAllBytes()));
            }
        }
        return result;
    }

    private static Map<String, Object> runtime() throws Exception {
        var result = new TreeMap<String, Object>();
        for (String key : List.of("java.runtime.version", "java.vendor", "java.vm.name", "os.name", "os.arch"))
            result.put(key, System.getProperty(key));
        Path home = Path.of(System.getProperty("java.home"));
        result.put("javaSha256", hashFile(home.resolve("bin/java")));
        result.put("modulesSha256", hashFile(home.resolve("lib/modules")));
        result.put("scope", "ISOLATED_HOST_JVM_NOT_PRODUCTION_IMAGE");
        return result;
    }

    private static Map<String, List<String>> readers() {
        var result = new TreeMap<String, List<String>>();
        for (String format : List.of("JPEG", "PNG", "WEBP")) {
            var names = new ArrayList<String>();
            var readers = ImageIO.getImageReadersByFormatName(format);
            while (readers.hasNext()) {
                var reader = readers.next();
                try { names.add(reader.getClass().getName()); } finally { reader.dispose(); }
            }
            result.put(format, names);
        }
        return result;
    }

    static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String hashFile(Path path) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024]; int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
