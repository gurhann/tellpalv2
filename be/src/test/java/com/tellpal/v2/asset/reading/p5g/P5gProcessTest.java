package com.tellpal.v2.asset.reading.p5g;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = "p5g.local.enabled", matches = "true")
class P5gProcessTest {
    @TempDir Path temporary;

    @org.junit.jupiter.api.BeforeEach void canonicalTemporary() throws Exception { temporary = temporary.toRealPath(); }

    @Test void effectiveBudgetsCannotExceedProfile() throws Exception {
        for (long[] limits : new long[][] {{0, P5gProfile.RSS}, {30_001, P5gProfile.RSS},
                {5000, 0}, {5000, P5gProfile.RSS + 1}}) {
            assertEquals("CHILD_BUDGET_INVALID", assertThrows(IOException.class,
                    () -> P5gSupervisor.execute(helper("empty"), limits[0], limits[1], temporary, "budget")).getMessage());
        }
        assertFalse(Files.exists(temporary.resolve("budget.stdout.json")));
    }

    @Test void nativeRssIsPositiveAndUnavailablePidDoesNotInventMemory() throws Exception {
        assertTrue(P5gRss.read(ProcessHandle.current().pid()) > 0);
        assertEquals(0, P5gRss.read(Integer.MAX_VALUE));
        assertThrows(IOException.class, () -> P5gRss.read(Long.MAX_VALUE));
    }

    @Test void realTimeoutKillsChildAndWaitsForExit() throws Exception {
        assertTrue(assertThrows(IOException.class, () -> P5gSupervisor.execute(helper("sleep"),
                700, P5gProfile.RSS, temporary, "timeout")).getMessage().startsWith("CHILD_TIMEOUT"));
        assertExited("timeout");
    }

    @Test void realRssLimitKillsChildAndWaitsForExit() throws Exception {
        String message = assertThrows(IOException.class, () -> P5gSupervisor.execute(helper("sleep"),
                5000, 20L * 1024 * 1024, temporary, "rss")).getMessage();
        assertTrue(message.startsWith("CHILD_RSS_LIMIT"));
        long pid = Long.parseLong(message.substring(message.indexOf(":PID=") + 5));
        assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
    }

    @Test void realNonzeroAndMissingResponseCannotSucceed() throws Exception {
        assertTrue(assertThrows(IOException.class, () -> P5gSupervisor.execute(helper("exit"),
                5000, P5gProfile.RSS, temporary, "exit")).getMessage().startsWith("CHILD_EXIT:7"));
        assertExited("exit");
        assertEquals("CHILD_RESPONSE_MISSING", assertThrows(IOException.class, () -> P5gSupervisor.execute(helper("empty"),
                5000, P5gProfile.RSS, temporary, "empty")).getMessage());
    }

    @Test void actualNetworkIsDeniedAndLogsAreCapped() throws Exception {
        var result = P5gSupervisor.execute(helper("network"), 5000, P5gProfile.RSS, temporary, "network");
        assertEquals(0, result.exit());
        assertTrue(Files.readString(temporary.resolve("network.stdout.json")).contains("NETWORK_DENIED"));
        assertTrue(assertThrows(IOException.class, () -> P5gSupervisor.execute(helper("flood"),
                5000, P5gProfile.RSS, temporary, "flood")).getMessage().startsWith("CHILD_LOG_LIMIT"));
        assertTrue(Files.size(temporary.resolve("flood.stdout.json")) + Files.size(temporary.resolve("flood.stderr.txt"))
                <= P5gProfile.RESPONSE);
        assertExited("flood");
    }

    @Test void correctCopyIsImmutableAndRereadRejectsChangedOrMissingOutputAndWrongPin() throws Exception {
        var items = P5gCorpus.load(Path.of(System.getProperty("p5g.report")));
        var item = items.getFirst(); Path output = temporary.resolve("copy.jpg");
        var proof = P5gRunner.verifyCopy(item, output, temporary);
        assertEquals(true, proof.get("fullRawRgbAndMetadataEquality"));
        assertEquals(item.candidateSha(), P5gProfile.fileSha(output));
        Path conflictLogs = Files.createDirectory(temporary.resolve("conflict"));
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
                () -> P5gRunner.verifyCopy(item, output, conflictLogs));
        assertEquals(item.candidateSha(), P5gProfile.fileSha(output));
        byte[] changed = Files.readAllBytes(output); changed[changed.length - 1] ^= 1;
        Files.write(output, changed);
        assertThrows(IOException.class, () -> P5gSupervisor.verify(item.source(), item.sourceSha(), item.sourceMime(),
                output, item.candidateSha(), item.candidateMime(), temporary, "tampered"));
        assertThrows(IOException.class, () -> P5gSupervisor.verify(item.source(), "0".repeat(64), item.sourceMime(),
                item.candidate(), item.candidateSha(), item.candidateMime(), temporary, "wrongpin"));
        assertThrows(IOException.class, () -> P5gSupervisor.verify(item.source(), item.sourceSha(), item.sourceMime(),
                temporary.resolve("missing"), item.candidateSha(), item.candidateMime(), temporary, "missing"));
    }

    @Test void reportPinAndOutputCollisionFailClosed() throws Exception {
        Path wrongReport = temporary.resolve("report.json"); Files.writeString(wrongReport, "{}");
        assertEquals("REPORT_PIN_REFUSED", assertThrows(IOException.class, () -> P5gCorpus.load(wrongReport)).getMessage());
        Path report = Path.of(System.getProperty("p5g.report"));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> P5gRunner.run(report,
                Path.of(System.getProperty("p5g.baseline")), Path.of(System.getProperty("p5g.repo")), temporary));
        assertFalse(Files.exists(temporary.resolve("manifest.json")));
    }

    @Test void failureAfterCreateHasDiagnosticAndNoCorpusSuccess() throws Exception {
        Path report = Path.of(System.getProperty("p5g.report")), output = temporary.resolve("failed-run");
        assertThrows(IOException.class, () -> P5gRunner.run(report,
                Path.of(System.getProperty("p5g.baseline")), Path.of(System.getProperty("p5g.repo")), output,
                (item, target, logs) -> P5gRunner.verifyCopy(new P5gCorpus.Item(item.assetId(), item.source(),
                        "0".repeat(64), item.sourceMime(), item.candidate(), item.candidateSha(),
                        item.candidateBytes(), item.candidateMime()), target, logs)));
        assertTrue(Files.exists(output.resolve("failure.json")));
        assertFalse(Files.exists(output.resolve("manifest.json")));
    }

    @Test void changedProtectionPinOrMembershipCannotCreateOutput() throws Exception {
        Path report = Path.of(System.getProperty("p5g.report"));
        Path repo = Path.of(System.getProperty("p5g.repo"));
        var original = P5gProfile.JSON.readTree(Files.readAllBytes(Path.of(System.getProperty("p5g.baseline"))));
        for (int mode = 0; mode < 3; mode++) {
            var baseline = original.deepCopy();
            var originals = (com.fasterxml.jackson.databind.node.ObjectNode) baseline.path("originals");
            String first = originals.properties().iterator().next().getKey();
            if (mode == 0) originals.put(first, "0".repeat(64));
            else if (mode == 1) originals.remove(first);
            else {
                String sha = originals.remove(first).asText();
                originals.put(temporary.resolve("missing-original").toString(), sha);
            }
            Path pin = temporary.resolve("baseline-" + mode + ".json");
            Files.write(pin, P5gProfile.JSON.writeValueAsBytes(baseline));
            Path output = temporary.resolve("refused-" + mode);
            assertThrows(IOException.class, () -> P5gRunner.run(report, pin, repo, output));
            assertFalse(Files.exists(output));
        }
    }

    @Test void changedProtectionAfterStartPreventsSuccessManifest() throws Exception {
        Path report = Path.of(System.getProperty("p5g.report"));
        Path protectedFile = temporary.resolve("protected-fixture"); Files.writeString(protectedFile, "before");
        var baseline = P5gProfile.JSON.readTree(Files.readAllBytes(Path.of(System.getProperty("p5g.baseline"))));
        var old = (com.fasterxml.jackson.databind.node.ObjectNode) baseline.path("preExisting");
        old.remove(old.properties().iterator().next().getKey());
        old.put(protectedFile.toString(), P5gProfile.fileSha(protectedFile));
        Path pin = temporary.resolve("synthetic-baseline.json"); Files.write(pin, P5gProfile.JSON.writeValueAsBytes(baseline));
        Path output = temporary.resolve("changed-during-run");
        var error = assertThrows(IOException.class, () -> P5gRunner.run(report, pin,
                Path.of(System.getProperty("p5g.repo")), output, (item, target, logs) -> {
                    Files.writeString(protectedFile, "after");
                    return new java.util.LinkedHashMap<>();
                }));
        assertTrue(error.getMessage().startsWith("PROTECTION_SHA:preExisting:"));
        assertFalse(Files.exists(output.resolve("manifest.json")));
        assertTrue(Files.readString(output.resolve("failure.json")).contains("PROTECTION_SHA:preExisting:"));
    }

    @Test void realChildRejectsCorrectPinsWithDifferentRgbOrMetadata() throws Exception {
        var rows = P5gCorpus.load(Path.of(System.getProperty("p5g.report"))).stream()
                .filter(item -> item.sourceMime().equals("image/png")).toList();
        var source = rows.getFirst(); var different = rows.get(1);
        assertTrue(assertThrows(IOException.class, () -> P5gSupervisor.verify(source.source(), source.sourceSha(),
                source.sourceMime(), different.candidate(), different.candidateSha(), different.candidateMime(),
                temporary, "different-rgb")).getMessage().startsWith("CHILD_EXIT:"));
        assertTrue(Files.readString(temporary.resolve("different-rgb.stderr.txt")).contains("RGB_MISMATCH"));
        byte[] original = P5gProfile.read(source.candidate(), P5gProfile.ENCODED);
        for (String metadata : List.of("XMP ", "EXIF")) {
            byte[] changed = original.clone(); int at = P5gCodecTest.chunk(changed, metadata) + 8;
            if (metadata.equals("XMP ")) {
                while (changed[at] != ' ') at++;
                changed[at] = '\t';
            } else changed[at + 77] ^= 1;
            Path candidate = temporary.resolve(metadata.strip() + ".webp"); Files.write(candidate, changed);
            String job = "different-" + metadata.strip();
            assertTrue(assertThrows(IOException.class, () -> P5gSupervisor.verify(source.source(), source.sourceSha(),
                    source.sourceMime(), candidate, P5gProfile.sha(changed), "image/webp", temporary, job))
                    .getMessage().startsWith("CHILD_EXIT:"));
            assertTrue(Files.readString(temporary.resolve(job + ".stderr.txt"))
                    .contains(metadata.equals("EXIF") ? "TIFF_MISMATCH" : "XMP_MISMATCH"));
        }
    }

    @Test void interruptWaitsForExitAndCapacityIsSingleJob() throws Exception {
        var outcome = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread owner = Thread.ofPlatform().start(() -> {
            try { P5gSupervisor.execute(helper("sleep"), 5000, P5gProfile.RSS, temporary, "interrupted"); }
            catch (Throwable error) { outcome.set(error); }
        });
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        Path log = temporary.resolve("interrupted.stdout.json");
        while ((!Files.exists(log) || Files.size(log) == 0) && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(Files.size(log) > 0);
        assertEquals("CHILD_CONCURRENCY_EXHAUSTED", assertThrows(IOException.class,
                () -> P5gSupervisor.execute(helper("empty"), 5000, P5gProfile.RSS, temporary, "overlap")).getMessage());
        owner.interrupt(); owner.join(3000);
        assertFalse(owner.isAlive()); assertInstanceOf(InterruptedException.class, outcome.get());
        assertExited("interrupted");
        assertEquals(0, P5gSupervisor.execute(helper("network"), 5000, P5gProfile.RSS, temporary, "after").exit());
    }

    private void assertExited(String job) throws Exception {
        String first = Files.readAllLines(temporary.resolve(job + ".stdout.json")).getFirst();
        assertFalse(ProcessHandle.of(Long.parseLong(first)).map(ProcessHandle::isAlive).orElse(false));
    }

    private static List<String> helper(String mode) {
        return List.of(Path.of(System.getProperty("java.home"), "bin/java").toString(), "-Xmx256m",
                "-Dp5g.local.enabled=true", "-cp", System.getProperty("java.class.path"), Helper.class.getName(), mode);
    }

    public static final class Helper {
        public static void main(String[] args) throws Exception {
            if (args[0].equals("runner")) { P5gRunner.main(java.util.Arrays.copyOfRange(args, 1, args.length)); return; }
            if (args[0].equals("empty")) return;
            System.out.println(ProcessHandle.current().pid()); System.out.flush();
            switch (args[0]) {
                case "sleep" -> Thread.sleep(10_000);
                case "exit" -> System.exit(7);
                case "flood" -> { byte[] bytes = new byte[8192]; for (int i = 0; i < 256; i++) System.out.write(bytes); }
                case "network" -> {
                    try (var socket = new Socket()) {
                        socket.connect(new InetSocketAddress("127.0.0.1", 9), 100);
                        throw new IllegalStateException("NETWORK_UNEXPECTEDLY_ALLOWED");
                    } catch (java.net.SocketException error) {
                        if (!error.getMessage().contains("Operation not permitted")) throw error;
                        System.out.println("NETWORK_DENIED");
                    }
                }
                default -> throw new IllegalArgumentException(args[0]);
            }
        }
    }
}
