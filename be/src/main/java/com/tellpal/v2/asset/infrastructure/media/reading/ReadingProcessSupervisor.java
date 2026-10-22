package com.tellpal.v2.asset.infrastructure.media.reading;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Oturum sahipliğini Java ancestry'den bağımsız tutan guardian'ı açıkça paketler. */
public final class ReadingProcessSupervisor {
    public static final String PROPERTY = "tellpal.reading.supervisor-directory";
    public static final String ENVIRONMENT = "TELLPAL_READING_SUPERVISOR_DIRECTORY";
    public static final String SOURCE_SHA = "b30cb810e9e7f16d1830b1de00f9d8d52d94a14fb299d733a267664fe8c60643";
    private ReadingProcessSupervisor() { }

    public static Path binary() throws IOException {
        String value = System.getProperty(PROPERTY, System.getenv(ENVIRONMENT));
        if (value == null || value.isBlank()) throw new IOException("PROCESS_SUPERVISOR_REQUIRED");
        Path path = Path.of(value).toAbsolutePath().normalize().resolve("reading-process-supervisor");
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || !Files.isExecutable(path)
                || Files.size(path) > 4 * 1024 * 1024) throw new IOException("PROCESS_SUPERVISOR_UNAVAILABLE");
        return path;
    }

    public static Map<String, Object> evidence() {
        try {
            Path binary = binary();
            Process process = new ProcessBuilder(binary.toString(), "--version").redirectErrorStream(true).start();
            String version;
            try {
                if (!process.waitFor(2, TimeUnit.SECONDS)) throw new IOException("PROCESS_SUPERVISOR_VERSION_TIMEOUT");
                version = new String(process.getInputStream().readNBytes(1024), java.nio.charset.StandardCharsets.US_ASCII).trim();
                if (process.exitValue() != 0 || !version.startsWith("tellpal-reading-supervisor-v1 " + SOURCE_SHA + " compiler="))
                    throw new IOException("PROCESS_SUPERVISOR_SOURCE_IDENTITY_MISMATCH");
            } finally { process.destroyForcibly(); process.waitFor(); }
            return Map.of("version", version, "binarySha256", ReadingPreservation.sha(Files.readAllBytes(binary)),
                    "sourceSha256", SOURCE_SHA, "compileFlags", "-std=c11 -O2 -Wall -Wextra -Werror",
                    "ownership", "SETSID_GROUP_GUARDIAN_STDIN_LIFELINE_WORKER_PID_HELD_UNTIL_GROUP_EXIT",
                    "linuxReap", "PR_SET_CHILD_SUBREAPER_WAITPID_ALL", "macosExit", "KERN_PROC_PGRP_NO_NONZOMBIE_MEMBERS",
                    "teardown", Map.of("monitorMillis", 10, "emptyConfirmations", 2, "signals", "SIGSTOP_THEN_SIGKILL_OWNED_GROUP"));
        } catch (IOException e) { throw new IllegalStateException("PROCESS_SUPERVISOR_UNAVAILABLE", e); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("PROCESS_SUPERVISOR_INTERRUPTED", e); }
    }

    static OwnedProcess start(List<String> command, Path log, Path members) throws IOException {
        evidence();
        var invocation = new ArrayList<>(List.of(binary().toString(), members.toString())); invocation.addAll(command);
        return new OwnedProcess(new ProcessBuilder(invocation).redirectErrorStream(true).redirectOutput(log.toFile()).start(), members);
    }

    /** destroy guardian'a sinyal göndermez; lifeline kapanışı sahipli grubun çıkışını bekler. */
    static final class OwnedProcess extends Process {
        private final Process guardian;
        private final Path members;
        OwnedProcess(Process guardian, Path members) { this.guardian = guardian; this.members = members; }
        List<Long> members() throws IOException {
            if (!Files.exists(members)) return List.of();
            return Files.readAllLines(members).stream().map(Long::parseLong).toList();
        }
        void finish() {
            boolean interrupted = Thread.interrupted();
            try { guardian.getOutputStream().close(); } catch (IOException ignored) { }
            for (;;) {
                try { guardian.waitFor(); break; } catch (InterruptedException e) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
        @Override public OutputStream getOutputStream() { return guardian.getOutputStream(); }
        @Override public InputStream getInputStream() { return guardian.getInputStream(); }
        @Override public InputStream getErrorStream() { return guardian.getErrorStream(); }
        @Override public int waitFor() throws InterruptedException { return guardian.waitFor(); }
        @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException { return guardian.waitFor(timeout, unit); }
        @Override public int exitValue() { return guardian.exitValue(); }
        @Override public long pid() { return guardian.pid(); }
        @Override public boolean isAlive() { return guardian.isAlive(); }
        @Override public void destroy() { finish(); }
        @Override public Process destroyForcibly() { finish(); return this; }
    }
}
