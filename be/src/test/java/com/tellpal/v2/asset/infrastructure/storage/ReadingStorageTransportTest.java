package com.tellpal.v2.asset.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.cloud.NoCredentials;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** LOCAL_SDK_FAULT_INJECTION: accepted loopback connections prove read stalls only. */
@Timeout(15)
class ReadingStorageTransportTest {
    @Test
    void realSdkMetadataStallFailsWithinBoundAndMakesOneAttempt() throws Exception {
        try (var endpoint = new Endpoint(Mode.METADATA_STALL)) {
            var adapter = endpoint.adapter();
            long start = System.nanoTime();
            assertThrows(RuntimeException.class, () -> adapter.withReadingDeadline(200).observeVersion("fixture.png"));
            assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 2_500);
            assertEquals(1, endpoint.metadata.get());
            assertEquals(0, endpoint.media.get());
        }
    }

    @Test
    void realSdkPartialMediaStallFailsWithinBoundWithPinnedGenerationAndOneAttempt() throws Exception {
        // The SDK can retry inside a body-read loop without consulting maxAttempts.
        // Run this fault in an owned JVM so a failed assertion cannot leak that loop.
        Path output = Files.createTempFile("p2-body-fault-", ".log");
        var child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp", System.getProperty("java.class.path"), StalledBodyChild.class.getName())
                .redirectOutput(output.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        boolean exited;
        try {
            exited = child.waitFor(2_500, TimeUnit.MILLISECONDS);
        } finally {
            child.destroyForcibly();
            assertTrue(child.waitFor(2, TimeUnit.SECONDS), "Fault child exit observed before returning");
        }
        String evidence = Files.readString(output);
        Files.delete(output);
        System.out.println("LOCAL_SDK_FAULT_INJECTION BODY_STALL CHILD_EXIT_OBSERVED true " + evidence.replace('\n', ' '));
        assertTrue(exited, "SDK_BODY_DEADLINE_NOT_ENFORCED: 300 ms reading limit exceeded 2500 ms; supervised child killed and reaped. " + evidence);
        assertEquals(0, child.exitValue());
        assertEquals(1, evidence.lines().filter(line -> line.startsWith("MEDIA_ATTEMPT ")).count());
        assertTrue(evidence.contains("BODY_FIRST_BYTE_FLUSHED"), "The HTTP body started before the timeout");
        assertTrue(evidence.contains("PINNED true"));
        assertTrue(evidence.contains("metadata=1"));
    }

    public static final class StalledBodyChild {
        public static void main(String[] args) throws Exception {
            java.util.logging.LogManager.getLogManager().reset();
            try (var endpoint = new Endpoint(Mode.BODY_STALL)) {
                var storage = new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(endpoint.adapter())));
                Thread reader = Thread.currentThread();
                Thread.ofPlatform().daemon(true).start(() -> {
                    try { Thread.sleep(1_000); }
                    catch (InterruptedException interrupted) { return; }
                    System.out.println("CLIENT_THREAD_SNAPSHOT_BEFORE_FIXTURE_CLEANUP");
                    for (var frame : reader.getStackTrace()) System.out.println("CLIENT_FRAME " + frame);
                    System.out.flush();
                });
                long start = System.nanoTime();
                boolean rejected = false;
                try { storage.read(ReadingStorageLiveProbe.PROVIDER, endpoint.object(), ReadingStorageLiveProbe.LIMIT, 300); }
                catch (Exception expected) { rejected = true; }
                long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
                System.out.println("SDK_TERMINATED elapsedMillis=" + elapsed + " metadata=" + endpoint.metadata.get());
                if (!rejected || elapsed > 2_000 || endpoint.media.get() != 1) System.exit(1);
            }
        }
    }

    @Test
    void readingCloneSetsTimeoutsAndOneAttemptWithoutChangingLegacyOptions() throws Exception {
        try (var endpoint = new Endpoint(Mode.NORMAL)) {
            var original = endpoint.sdk;
            var originalRetry = original.getOptions().getRetrySettings();
            var originalTransport = original.getOptions().getTransportOptions();
            var originalStrategy = strategy(original);
            var bounded = endpoint.adapter().withReadingDeadline(321);
            var field = FirebaseStorageAssetClient.class.getDeclaredField("storage");
            field.setAccessible(true);
            var cloned = (Storage) field.get(bounded);
            var options = cloned.getOptions();
            var transport = (HttpTransportOptions) options.getTransportOptions();
            assertEquals(321, transport.getConnectTimeout());
            assertEquals(321, transport.getReadTimeout());
            assertEquals(1, options.getRetrySettings().getMaxAttempts());
            assertEquals(Duration.ofMillis(321), options.getRetrySettings().getTotalTimeoutDuration());
            assertEquals(Duration.ofMillis(321), options.getRetrySettings().getInitialRpcTimeoutDuration());
            assertEquals(Duration.ofMillis(321), options.getRetrySettings().getMaxRpcTimeoutDuration());
            assertSame(originalRetry, original.getOptions().getRetrySettings());
            assertSame(originalTransport, original.getOptions().getTransportOptions());
            assertSame(originalStrategy, strategy(original));
            var noRetry = com.google.cloud.storage.StorageRetryStrategy.getDefaultStorageRetryStrategy().getNonidempotentHandler();
            assertSame(noRetry, strategy(cloned).getIdempotentHandler());
            assertSame(noRetry, strategy(cloned).getNonidempotentHandler());
            assertFalse(strategy(cloned).getIdempotentHandler().shouldRetry(new java.net.SocketTimeoutException(), null));
            assertFalse(strategy(cloned).getNonidempotentHandler().shouldRetry(new com.google.cloud.storage.StorageException(503, "unavailable"), null));
            assertTrue(originalStrategy.getIdempotentHandler().shouldRetry(new com.google.cloud.storage.StorageException(503, "unavailable"), null));
            assertEquals(0, endpoint.requests.size());
        }
    }

    private static com.google.cloud.storage.StorageRetryStrategy strategy(Storage storage) throws Exception {
        var field = storage.getOptions().getClass().getDeclaredField("retryAlgorithmManager");
        field.setAccessible(true);
        var manager = field.get(storage.getOptions());
        var strategy = manager.getClass().getDeclaredField("retryStrategy");
        strategy.setAccessible(true);
        return (com.google.cloud.storage.StorageRetryStrategy) strategy.get(manager);
    }

    enum Mode { NORMAL, METADATA_STALL, BODY_STALL, AMBIGUOUS_CREATE_FAILURE }

    /** Real HTTP SDK fixture; it supplies no credentials and binds exclusively to loopback. */
    static final class Endpoint implements AutoCloseable {
        final byte[] bytes = ReadingStorageLiveProbe.fixture();
        final AtomicInteger metadata = new AtomicInteger();
        final AtomicInteger media = new AtomicInteger();
        final AtomicInteger creates = new AtomicInteger();
        final List<String> queries = new CopyOnWriteArrayList<>();
        final List<String> requests = new CopyOnWriteArrayList<>();
        final List<byte[]> uploads = new CopyOnWriteArrayList<>();
        final HttpServer server;
        final Storage sdk;
        private final Mode mode;
        private final CountDownLatch release = new CountDownLatch(1);
        private final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool();

        Endpoint(Mode mode) throws IOException {
            this.mode = mode;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.setExecutor(executor);
            server.start();
            sdk = StorageOptions.newBuilder().setProjectId("probe-local")
                    .setHost("http://127.0.0.1:" + server.getAddress().getPort())
                    .setCredentials(NoCredentials.getInstance()).build().getService();
        }

        FirebaseStorageAssetClient adapter() {
            return new FirebaseStorageAssetClient(sdk, null,
                    new AssetStorageFirebaseProperties(false, "probe-local", "probe-bucket", null, "local", null, null, null));
        }

        VersionedStorageObject object() { return new VersionedStorageObject("fixture.png", "17", "image/png", bytes.length); }

        private void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getRawQuery();
            query = query == null ? "" : query;
            queries.add(query);
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            try {
                if (exchange.getRequestMethod().equals("POST")) {
                    uploads.add(exchange.getRequestBody().readAllBytes());
                    int attempt = creates.incrementAndGet();
                    if (attempt == 2 && mode == Mode.AMBIGUOUS_CREATE_FAILURE) {
                        send(exchange, 503, "{\"error\":{\"code\":503,\"message\":\"unavailable\"}}".getBytes(StandardCharsets.UTF_8));
                        return;
                    }
                    if (attempt > 1) { send(exchange, 412, "{\"error\":{\"code\":412,\"message\":\"precondition\"}}".getBytes(StandardCharsets.UTF_8)); return; }
                    send(exchange, 200, metadataJson());
                    return;
                }
                if (query.contains("alt=media")) {
                    int attempt = media.incrementAndGet();
                    if (mode == Mode.BODY_STALL) {
                        System.out.println("MEDIA_ATTEMPT " + attempt + " PINNED "
                                + (query.contains("generation=17") && query.contains("ifGenerationMatch=17")));
                        System.out.flush();
                    }
                    if (!query.contains("generation=17") || !query.contains("ifGenerationMatch=17")) { send(exchange, 400, new byte[0]); return; }
                    if (mode == Mode.BODY_STALL) {
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes, 0, 1);
                        exchange.getResponseBody().flush();
                        System.out.println("BODY_FIRST_BYTE_FLUSHED");
                        System.out.flush();
                        awaitRelease();
                        return;
                    }
                    send(exchange, 200, bytes);
                    return;
                }
                metadata.incrementAndGet();
                if (mode == Mode.METADATA_STALL) { awaitRelease(); return; }
                if (query.contains("generation=") && !query.contains("generation=17")) {
                    send(exchange, 404, "{\"error\":{\"code\":404,\"message\":\"missing\"}}".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                send(exchange, 200, metadataJson());
            } finally { exchange.close(); }
        }

        private byte[] metadataJson() {
            return ("{\"kind\":\"storage#object\",\"bucket\":\"probe-bucket\",\"name\":\"fixture.png\","
                    + "\"generation\":\"17\",\"metageneration\":\"1\",\"contentType\":\"image/png\",\"size\":\"" + bytes.length + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
        }

        private void awaitRelease() {
            try { release.await(5, TimeUnit.SECONDS); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }

        @Override public void close() throws InterruptedException {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS), "Loopback fault server exited");
        }
    }
}
