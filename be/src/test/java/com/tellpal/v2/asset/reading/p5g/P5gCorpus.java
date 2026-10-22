package com.tellpal.v2.asset.reading.p5g;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Yalnız pinli rapordaki 41 kaynağı seçer; fixture veya medya dizini keşfetmez. */
final class P5gCorpus {
    record Item(long assetId, Path source, String sourceSha, String sourceMime,
                Path candidate, String candidateSha, long candidateBytes, String candidateMime) { }
    private P5gCorpus() { }

    static List<Item> load(Path reportPath) throws Exception {
        byte[] reportBytes = P5gProfile.read(reportPath, P5gProfile.RESPONSE);
        P5gProfile.require(P5gProfile.sha(reportBytes).equals(P5gProfile.REPORT_SHA), "REPORT_PIN_REFUSED");
        JsonNode report = P5gProfile.JSON.readTree(reportBytes);
        P5gProfile.require(report.path("sources").isArray() && report.path("sources").size() == 41, "CORPUS_COUNT");
        var items = new ArrayList<Item>(); var ids = new HashSet<Long>(); int jpeg = 0;
        for (JsonNode row : report.path("sources")) {
            long id = row.path("assetId").asLong();
            P5gProfile.require(id > 0 && ids.add(id), "CORPUS_ID");
            boolean isJpeg = row.path("source").path("format").asText().equals("JPEG");
            P5gProfile.require(isJpeg || row.path("source").path("format").asText().equals("PNG"), "CORPUS_FORMAT");
            Path source = child(reportPath.getParent(), row.path("sourceFile").asText());
            verify(source, row.path("sourceBytes").asLong(), row.path("sourceSha256").asText());
            for (String format : List.of("PNG", "WEBP")) {
                var candidate = row.path("candidates").path(format);
                verify(child(reportPath.getParent(), candidate.path("file").asText()), candidate.path("bytes").asLong(),
                        candidate.path("sha256").asText());
            }
            var selected = row.path("candidates").path("WEBP");
            items.add(new Item(id, source, row.path("sourceSha256").asText(), isJpeg ? "image/jpeg" : "image/png",
                    isJpeg ? source : child(reportPath.getParent(), selected.path("file").asText()),
                    isJpeg ? row.path("sourceSha256").asText() : selected.path("sha256").asText(),
                    isJpeg ? row.path("sourceBytes").asLong() : selected.path("bytes").asLong(),
                    isJpeg ? "image/jpeg" : "image/webp"));
            if (isJpeg) jpeg++;
        }
        P5gProfile.require(jpeg == 17 && items.size() - jpeg == 24, "CORPUS_FORMAT_COUNTS");
        return items;
    }

    static void verify(Path path, long size, String sha) throws IOException {
        byte[] bytes = P5gProfile.read(path, P5gProfile.ENCODED);
        P5gProfile.require(bytes.length == size && P5gProfile.sha(bytes).equals(sha), "CORPUS_FILE_PIN");
    }

    static Map<String, String> protection(JsonNode baseline, Path repo, Path report) throws Exception {
        var result = new TreeMap<String, String>();
        var counts = Map.of("preExisting", 88, "originals", 41, "p5c", 124, "p5fEvidence", 7);
        for (var group : counts.entrySet()) {
            JsonNode files = baseline.path(group.getKey());
            P5gProfile.require(files.isObject() && files.size() == group.getValue(), "PROTECTION_MEMBERSHIP:" + group.getKey());
            var entries = files.properties();
            for (var entry : entries) {
                Path path = Path.of(entry.getKey());
                if (!path.isAbsolute()) path = repo.resolve(path);
                path = path.toAbsolutePath().normalize();
                P5gProfile.require(path.equals(path.toRealPath()), "PROTECTION_ALIAS");
                String observed = P5gProfile.fileSha(path);
                P5gProfile.require(entry.getValue().asText().equals(observed), "PROTECTION_SHA:" + group.getKey() + ":" + path);
                result.put(group.getKey() + ":" + path, observed);
            }
        }
        P5gProfile.require(baseline.path("p5c").path(report.toString()).asText().equals(P5gProfile.REPORT_SHA), "BASELINE_REPORT_BINDING");
        return result;
    }

    static void bindOriginals(List<Item> items, JsonNode baseline) throws IOException {
        var originalHashes = new HashSet<String>();
        baseline.path("originals").properties().forEach(e -> originalHashes.add(e.getValue().asText()));
        P5gProfile.require(originalHashes.size() == 41, "ORIGINAL_PIN_DUPLICATE");
        for (Item item : items) P5gProfile.require(originalHashes.remove(item.sourceSha()), "ORIGINAL_PIN_MISSING");
        P5gProfile.require(originalHashes.isEmpty(), "ORIGINAL_PIN_EXTRA");
    }

    private static Path child(Path parent, String name) throws IOException {
        P5gProfile.require(!name.isBlank() && Path.of(name).getNameCount() == 1 && !name.equals(".") && !name.equals(".."), "CORPUS_NAME");
        Path path = parent.resolve(name).toAbsolutePath().normalize();
        P5gProfile.require(parent.equals(path.getParent()), "CORPUS_PATH"); return path;
    }
}
