import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** JDK-only harness; all adapter/wrapper/SDK classes must load from the executable application JAR. */
public final class PackagedStorageProbe {
    private static final String PACKAGE = "com.tellpal.v2.asset.infrastructure.storage.";
    private static final int[] PIXELS = {0xff0000, 0x00ff00, 0x0000ff, 0x123456, 0xfedcba, 0x010203};
    private static Class<?> type(String name) throws Exception { return Class.forName(name); }
    private static Object call(Object target, String name, Class<?>[] parameters, Object... arguments) throws Exception {
        var method = target.getClass().getMethod(name, parameters);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }
    private static Object construct(String name, Class<?>[] parameters, Object... arguments) throws Exception {
        var constructor = type(name).getDeclaredConstructor(parameters);
        constructor.setAccessible(true);
        return constructor.newInstance(arguments);
    }

    public static void main(String[] args) {
        java.util.logging.LogManager.getLogManager().reset();
        Thread.ofPlatform().daemon(true).start(() -> {
            try { Thread.sleep(15_000); Runtime.getRuntime().halt(124); }
            catch (InterruptedException ignored) { }
        });
        try {
            if (args.length != 0) throw new IllegalStateException();
            String origin = type(PACKAGE + "FirebaseStorageAssetClient").getResource("FirebaseStorageAssetClient.class").toString();
            if (!origin.contains("BOOT-INF/classes")) throw new IllegalStateException();
            System.out.println("SCOPE PACKAGED_LOCAL_SDK_FAULT_INJECTION NO_CREDENTIALS NO_LIVE_GCS NO_PRODUCTION_ADMISSION");
            System.out.println("RUNTIME " + System.getProperty("java.runtime.version") + " " + System.getProperty("java.vendor")
                    + " " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            System.out.println("ADAPTER_ORIGIN " + origin);
            run("NORMAL"); run("METADATA_STALL"); run("BODY_STALL");
            System.out.println("RESULT PASS packaged actual adapter/SDK pinned bytes and inactivity stalls; absolute slow-trickle bound DEFERRED");
        } catch (Throwable failure) {
            System.out.println("RESULT FAILED PACKAGED_STORAGE_PROBE");
            System.exit(1);
        }
    }

    private static void run(String mode) throws Exception {
        try (var endpoint = new Endpoint(mode)) {
            var builder = type("com.google.cloud.storage.StorageOptions").getMethod("newBuilder").invoke(null);
            call(builder, "setProjectId", new Class<?>[] {String.class}, "probe-local");
            call(builder, "setHost", new Class<?>[] {String.class}, "http://127.0.0.1:" + endpoint.server.getAddress().getPort());
            var noCredentials = type("com.google.cloud.NoCredentials").getMethod("getInstance").invoke(null);
            call(builder, "setCredentials", new Class<?>[] {type("com.google.auth.Credentials")}, noCredentials);
            Object sdk = call(call(builder, "build", new Class<?>[0]), "getService", new Class<?>[0]);
            Object properties = construct(PACKAGE + "AssetStorageFirebaseProperties",
                    new Class<?>[] {boolean.class, String.class, String.class, String.class, String.class,
                            java.time.Duration.class, java.time.Duration.class, java.time.Duration.class},
                    false, "probe-local", "probe-bucket", null, "local", null, null, null);
            Object adapter = construct(PACKAGE + "FirebaseStorageAssetClient",
                    new Class<?>[] {type("com.google.cloud.storage.Storage"), type("com.google.auth.oauth2.ServiceAccountCredentials"), properties.getClass()},
                    sdk, null, properties);
            Object registry = construct(PACKAGE + "AssetStorageClientRegistry", new Class<?>[] {List.class}, List.of(adapter));
            Object wrapper = construct(PACKAGE + "ReadingObjectStorage", new Class<?>[] {registry.getClass()}, registry);
            Class<?> providerType = type("com.tellpal.v2.asset.domain.StorageProvider");
            Object provider = providerType.getField("FIREBASE_STORAGE").get(null);
            Object object = construct(PACKAGE + "VersionedStorageObject", new Class<?>[] {String.class, String.class, String.class, long.class},
                    "fixture.png", "17", "image/png", (long) endpoint.png.length);
            long started = System.nanoTime();
            byte[] actual = null;
            Throwable rejected = null;
            try {
                if (mode.equals("METADATA_STALL")) call(wrapper, "observe", new Class<?>[] {providerType, String.class, long.class}, provider, "fixture.png", 200L);
                else actual = (byte[]) call(wrapper, "read", new Class<?>[] {providerType, object.getClass(), long.class, long.class}, provider, object, 65_536L, mode.equals("NORMAL") ? 3000L : 300L);
            } catch (InvocationTargetException failure) { rejected = failure.getCause(); }
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (elapsed >= 2500 || endpoint.metadata.get() != 1) throw new IllegalStateException();
            if (mode.equals("NORMAL")) {
                if (rejected != null || !Arrays.equals(actual, endpoint.png)) throw new IllegalStateException();
                var decoded = ImageIO.read(new ByteArrayInputStream(actual));
                if (decoded == null || decoded.getWidth() != 3 || decoded.getHeight() != 2) throw new IllegalStateException();
                for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++)
                    if ((decoded.getRGB(x, y) & 0xffffff) != PIXELS[y * 3 + x]) throw new IllegalStateException();
                System.out.println("CONTENT sentinels=6 sha256=" + HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(actual)));
            } else if (rejected == null) throw new IllegalStateException();
            int media = mode.equals("METADATA_STALL") ? 0 : 1;
            if (endpoint.media.get() != media || !endpoint.pinned || (mode.equals("BODY_STALL") && endpoint.firstByte.getCount() != 0))
                throw new IllegalStateException();
            System.out.println("CASE " + mode + " PASS elapsedMillis=" + elapsed + " metadata=" + endpoint.metadata.get()
                    + " media=" + endpoint.media.get() + " pinned=" + endpoint.pinned + " firstBodyByteFlushed=" + (endpoint.firstByte.getCount() == 0));
        }
    }

    private static final class Endpoint implements AutoCloseable {
        final HttpServer server;
        final byte[] png;
        final AtomicInteger metadata = new AtomicInteger(), media = new AtomicInteger();
        final CountDownLatch release = new CountDownLatch(1), firstByte = new CountDownLatch(1);
        final java.util.concurrent.ExecutorService executor = Executors.newCachedThreadPool();
        volatile boolean pinned = true;
        final String mode;
        Endpoint(String mode) throws Exception {
            this.mode = mode;
            var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++) image.setRGB(x, y, PIXELS[y * 3 + x]);
            var output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) throw new IllegalStateException();
            png = output.toByteArray();
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle); server.setExecutor(executor); server.start();
        }
        void handle(HttpExchange exchange) throws IOException {
            String query = String.valueOf(exchange.getRequestURI().getRawQuery());
            try {
                if (query.contains("alt=media")) {
                    media.incrementAndGet();
                    pinned &= query.contains("generation=17") && query.contains("ifGenerationMatch=17");
                    exchange.sendResponseHeaders(200, png.length);
                    if (mode.equals("BODY_STALL")) {
                        exchange.getResponseBody().write(png, 0, 1); exchange.getResponseBody().flush(); firstByte.countDown(); await();
                    } else exchange.getResponseBody().write(png);
                    return;
                }
                metadata.incrementAndGet();
                if (mode.equals("METADATA_STALL")) { await(); return; }
                byte[] body = ("{\"kind\":\"storage#object\",\"bucket\":\"probe-bucket\",\"name\":\"fixture.png\",\"generation\":\"17\",\"metageneration\":\"1\",\"contentType\":\"image/png\",\"size\":\"" + png.length + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body);
            } finally { exchange.close(); }
        }
        void await() {
            try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
        @Override public void close() throws InterruptedException {
            release.countDown(); server.stop(0); executor.shutdownNow();
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) throw new IllegalStateException();
        }
    }
}
