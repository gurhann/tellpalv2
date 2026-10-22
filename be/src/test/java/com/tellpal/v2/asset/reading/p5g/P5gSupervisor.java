package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Her child tek iş yapar; sınır aşımında gerçek exit görülmeden kapasite bırakılmaz. */
final class P5gSupervisor {
    record Result(long pid, int exit, long peakRssBytes, long elapsedMillis, String stdoutSha256, String stderrSha256) { }
    private static final java.util.concurrent.Semaphore CAPACITY = new java.util.concurrent.Semaphore(1);
    private P5gSupervisor() { }

    static Result verify(Path source, String sourceSha, String sourceMime, Path candidate, String candidateSha,
                         String candidateMime, Path logDirectory, String job) throws Exception {
        P5gProfile.enabled();
        var command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin/java").toString(),
                "-Xmx256m", "-XX:MaxDirectMemorySize=32m", "-XX:ActiveProcessorCount=1", "-Djava.awt.headless=true",
                "-Dp5g.local.enabled=true", "-cp", System.getProperty("java.class.path"), P5gChild.class.getName(),
                source.toString(), sourceSha, sourceMime, candidate.toString(), candidateSha, candidateMime));
        return execute(command, P5gProfile.TIMEOUT_MILLIS, P5gProfile.RSS, logDirectory, job);
    }

    static Result execute(List<String> program, long timeoutMillis, long rssLimit, Path logs, String job) throws Exception {
        P5gProfile.enabled();
        P5gProfile.require(timeoutMillis > 0 && timeoutMillis <= P5gProfile.TIMEOUT_MILLIS
                && rssLimit > 0 && rssLimit <= P5gProfile.RSS, "CHILD_BUDGET_INVALID");
        P5gProfile.require(CAPACITY.tryAcquire(), "CHILD_CONCURRENCY_EXHAUSTED");
        try { return executeClaimed(program, timeoutMillis, rssLimit, logs, job); }
        finally { CAPACITY.release(); }
    }

    private static Result executeClaimed(List<String> program, long timeoutMillis, long rssLimit, Path logs, String job) throws Exception {
        P5gProfile.require(Files.isExecutable(Path.of("/usr/bin/sandbox-exec")), "NETWORK_SANDBOX_UNAVAILABLE");
        var command = new ArrayList<>(List.of("/usr/bin/sandbox-exec", "-p", "(version 1)(allow default)(deny network*)"));
        command.addAll(program);
        Path stdout = logs.resolve(job + ".stdout.json"), stderr = logs.resolve(job + ".stderr.txt");
        Files.createFile(stdout); Files.createFile(stderr);
        long start = System.nanoTime(), peak = 0; String failure = null;
        var builder = new ProcessBuilder(command);
        builder.environment().keySet().removeIf(key -> key.equals("JAVA_TOOL_OPTIONS") || key.equals("JDK_JAVA_OPTIONS")
                || key.equals("_JAVA_OPTIONS"));
        Process child = builder.start();
        var budget = new LogBudget();
        Thread out = pump(child.getInputStream(), stdout, budget);
        Thread err = pump(child.getErrorStream(), stderr, budget);
        try {
            while (child.isAlive()) {
                long rss = P5gRss.read(child.pid()); peak = Math.max(peak, rss);
                failure = violation(Duration.ofNanos(System.nanoTime() - start).toMillis(), timeoutMillis,
                        rss, rssLimit, budget.overflow ? P5gProfile.RESPONSE + 1L : budget.count);
                if (failure != null) break;
                child.waitFor(20, TimeUnit.MILLISECONDS);
            }
        } finally {
            if (child.isAlive()) {
                child.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
                child.destroyForcibly();
            }
            boolean interrupted = Thread.interrupted();
            while (true) {
                try { child.waitFor(); break; }
                catch (InterruptedException error) { interrupted = true; child.destroyForcibly(); }
            }
            try { out.join(2000); err.join(2000); }
            catch (InterruptedException error) { interrupted = true; }
            if (interrupted) Thread.currentThread().interrupt();
        }
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        P5gProfile.require(!out.isAlive() && !err.isAlive() && budget.failure == null, "CHILD_LOG_CAPTURE_FAILED");
        if (failure == null) failure = violation(elapsed, timeoutMillis, peak, rssLimit,
                budget.overflow ? P5gProfile.RESPONSE + 1L : budget.count);
        P5gProfile.require(failure == null, failure == null ? "" : failure + ":EXIT=" + child.exitValue() + ":PID=" + child.pid());
        P5gProfile.require(child.exitValue() == 0, "CHILD_EXIT:" + child.exitValue() + ":" + stderr);
        P5gProfile.require(Files.size(stdout) > 0, "CHILD_RESPONSE_MISSING");
        P5gProfile.require(peak > 0, "RSS_SAMPLE_REQUIRED");
        return new Result(child.pid(), child.exitValue(), peak, elapsed, P5gProfile.fileSha(stdout), P5gProfile.fileSha(stderr));
    }

    static String violation(long elapsed, long timeout, long rss, long rssLimit, long logBytes) {
        if (elapsed > timeout) return "CHILD_TIMEOUT";
        if (rss > rssLimit) return "CHILD_RSS_LIMIT";
        if (logBytes > P5gProfile.RESPONSE) return "CHILD_LOG_LIMIT";
        return null;
    }

    private static final class LogBudget {
        volatile long count;
        volatile boolean overflow;
        volatile IOException failure;
        synchronized void write(java.io.OutputStream out, byte[] bytes, int length) throws IOException {
            int accepted = (int) Math.min(length, P5gProfile.RESPONSE - count);
            out.write(bytes, 0, accepted); count += accepted;
            if (accepted < length) overflow = true;
        }
    }

    private static Thread pump(java.io.InputStream input, Path target, LogBudget budget) {
        Thread thread = Thread.ofPlatform().daemon().start(() -> {
            try (input; var out = Files.newOutputStream(target, java.nio.file.StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = input.read(buffer)) != -1) budget.write(out, buffer, n);
            } catch (IOException error) { budget.failure = error; }
        });
        return thread;
    }

}
