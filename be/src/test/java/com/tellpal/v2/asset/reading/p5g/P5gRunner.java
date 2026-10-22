package com.tellpal.v2.asset.reading.p5g;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Açık rapor/baseline/yeni çıktı diziniyle çalışan create-only yerel CLI'dır. */
public final class P5gRunner {
    private P5gRunner() { }

    /** Dört argüman: pinli rapor, koruma baseline'ı, repo ve henüz bulunmayan çıktı dizini. */
    public static void main(String[] args) throws Exception {
        P5gProfile.require(args.length == 4, "REPORT_BASELINE_REPO_NEW_OUTPUT_REQUIRED");
        run(Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
    }

    @FunctionalInterface
    interface Verifier {
        Map<String, Object> verify(P5gCorpus.Item item, Path target, Path logs) throws Exception;
    }

    static Map<String, Object> run(Path report, Path baselinePath, Path repo, Path output) throws Exception {
        return run(report, baselinePath, repo, output, P5gRunner::verifyCopy);
    }

    static Map<String, Object> run(Path report, Path baselinePath, Path repo, Path output, Verifier verifier) throws Exception {
        P5gProfile.enabled();
        report = report.toAbsolutePath().normalize(); repo = repo.toRealPath();
        baselinePath = baselinePath.toAbsolutePath().normalize(); output = output.toAbsolutePath().normalize();
        P5gProfile.require(output.getParent().equals(output.getParent().toRealPath()), "OUTPUT_PARENT_ALIAS");
        P5gProfile.require(!output.startsWith(repo), "MEDIA_MUST_REMAIN_OUTSIDE_REPOSITORY");
        byte[] baselineBytes = P5gProfile.read(baselinePath, P5gProfile.RESPONSE);
        JsonNode baseline = P5gProfile.JSON.readTree(baselineBytes);
        Map<String, String> before = P5gCorpus.protection(baseline, repo, report);
        List<P5gCorpus.Item> items = P5gCorpus.load(report); P5gCorpus.bindOriginals(items, baseline);
        Files.createDirectory(output); Files.createDirectory(output.resolve("logs"));
        var manifest = new LinkedHashMap<String, Object>(); var rows = new ArrayList<Map<String, Object>>();
        manifest.put("scope", "P5G_LOCAL_SERVER_CODEC_COMPATIBILITY");
        manifest.put("productionAdmission", "NOT_ADMITTED"); manifest.put("productionJreContainer", "NOT_RUN");
        manifest.put("physicalColorApproval", "NOT_RUN"); manifest.put("p2P3Acceptance", "UNCHANGED");
        manifest.put("p5cReportSha256", P5gProfile.REPORT_SHA); manifest.put("baselineSha256", P5gProfile.sha(baselineBytes));
        try {
            var identity = P5gIdentity.capture(); manifest.put("identity", identity);
            long total = 0;
            for (P5gCorpus.Item item : items) {
                total = Math.addExact(total, item.candidateBytes());
                P5gProfile.require(total <= P5gProfile.OUTPUT - P5gProfile.RESPONSE, "TOTAL_OUTPUT_LIMIT");
                P5gProfile.require(directoryBytes(output) + item.candidateBytes() + 2L * P5gProfile.RESPONSE
                        <= P5gProfile.OUTPUT - P5gProfile.RESPONSE, "TOTAL_OUTPUT_RESERVATION_LIMIT");
                String suffix = item.candidateMime().equals("image/jpeg") ? ".jpg" : ".webp";
                Path target = output.resolve(item.assetId() + suffix);
                var row = verifier.verify(item, target, output.resolve("logs"));
                row.put("fingerprint", P5gIdentity.fingerprint(identity, item.sourceSha(), item.candidateSha()));
                rows.add(row);
                P5gProfile.require(directoryBytes(output) <= P5gProfile.OUTPUT - P5gProfile.RESPONSE, "TOTAL_OUTPUT_LIMIT");
            }
            Map<String, String> after = P5gCorpus.protection(baseline, repo, report);
            P5gProfile.require(before.equals(after), "PROTECTION_CHANGED");
            P5gProfile.require(rows.size() == 41, "OUTPUT_MEMBERSHIP");
            for (var row : rows) P5gCorpus.verify(output.resolve((String) row.get("file")),
                    ((Number) row.get("bytes")).longValue(), (String) row.get("sha256"));
            manifest.put("status", "LOCAL_COMPATIBILITY_COMPLETE"); manifest.put("rows", rows);
            manifest.put("filesVerified", 41); manifest.put("jpegByteCopies", 17); manifest.put("losslessWebpByteCopies", 24);
            manifest.put("mediaBytes", total); manifest.put("preservation", Map.of("beforeEqualsAfter", true,
                    "preExisting", 88, "originals", 41, "p5c", 124, "p5fEvidence", 7,
                    "beforeSha256", P5gProfile.sha(P5gProfile.JSON.writeValueAsBytes(before)),
                    "afterSha256", P5gProfile.sha(P5gProfile.JSON.writeValueAsBytes(after))));
            byte[] bytes = P5gProfile.JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
            P5gProfile.require(bytes.length <= P5gProfile.RESPONSE, "MANIFEST_LIMIT");
            Files.write(output.resolve("manifest.json"), bytes, StandardOpenOption.CREATE_NEW);
            return manifest;
        } catch (Exception error) {
            manifest.put("status", "FAILED"); manifest.put("diagnostic", error.toString()); manifest.put("verifiedPartialRows", rows);
            try {
                manifest.put("preservationAfterFailure", before.equals(P5gCorpus.protection(baseline, repo, report)));
            } catch (Exception protectionError) { manifest.put("protectionDiagnostic", protectionError.toString()); }
            Files.write(output.resolve("failure.json"), P5gProfile.JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest),
                    StandardOpenOption.CREATE_NEW);
            throw error;
        }
    }

    static Map<String, Object> verifyCopy(P5gCorpus.Item item, Path target, Path logs) throws Exception {
        var initial = P5gSupervisor.verify(item.source(), item.sourceSha(), item.sourceMime(), item.candidate(),
                item.candidateSha(), item.candidateMime(), logs, item.assetId() + "-input");
        byte[] bytes = P5gProfile.read(item.candidate(), P5gProfile.ENCODED);
        P5gProfile.require(bytes.length == item.candidateBytes() && P5gProfile.sha(bytes).equals(item.candidateSha()), "COPY_PIN");
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        P5gCorpus.verify(target, item.candidateBytes(), item.candidateSha());
        var reread = P5gSupervisor.verify(item.source(), item.sourceSha(), item.sourceMime(), target,
                item.candidateSha(), item.candidateMime(), logs, item.assetId() + "-reread");
        JsonNode proof = P5gProfile.JSON.readTree(P5gProfile.read(logs.resolve(item.assetId() + "-reread.stdout.json"), P5gProfile.RESPONSE));
        P5gProfile.require(proof.path("status").asText().equals("VERIFIED")
                && proof.path("candidateSha256").asText().equals(item.candidateSha())
                && proof.path("sourceSha256").asText().equals(item.sourceSha())
                && proof.path("rgbBytesCompared").asLong() == 14_267_808, "CHILD_RESPONSE_INVALID");
        var row = new LinkedHashMap<String, Object>();
        row.put("assetId", item.assetId()); row.put("file", target.getFileName().toString());
        row.put("mime", item.candidateMime()); row.put("bytes", item.candidateBytes()); row.put("sha256", item.candidateSha());
        row.put("sourceSha256", item.sourceSha()); row.put("fullRawRgbAndMetadataEquality", true);
        row.put("inputProcess", initial); row.put("rereadProcess", reread); row.put("rereadProof", proof);
        return row;
    }

    private static long directoryBytes(Path directory) throws IOException {
        try (var files = Files.walk(directory)) {
            long total = 0;
            for (Path path : files.filter(Files::isRegularFile).toList()) total = Math.addExact(total, Files.size(path));
            return total;
        }
    }
}
