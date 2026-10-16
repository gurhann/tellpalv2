package com.tellpal.v2.asset.infrastructure.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.StorageOptions;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;

/** Explicitly invoked diagnostic; ordinary test discovery never executes cloud operations. */
public final class ReadingStorageLiveProbe {
    static final long LIMIT = 65_536;
    static final long OPERATION_MILLIS = 3_000;
    static final StorageProvider PROVIDER = StorageProvider.FIREBASE_STORAGE;

    private ReadingStorageLiveProbe() { }

    public static void main(String[] args) {
        java.util.logging.LogManager.getLogManager().reset();
        int code = run(args, System.out);
        System.exit(code);
    }

    static int run(String[] args, PrintStream output) {
        ProbeConfiguration config;
        try {
            config = ProbeConfiguration.parse(args);
        } catch (Exception refused) {
            output.println("REFUSED_CONFIGURATION");
            return 2;
        }
        if (!config.execute()) {
            output.println("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK");
            output.println("PROPOSED_SCOPE local/reading/p2-storage-probe/<generated-UUID>/fixture.png");
            output.println("CREATE_ATTEMPTS 4 RETAINED_OBJECTS_MAX 1 OPERATION_MILLIS 3000 RUN_SECONDS 60");
            output.println("LIVE_GCS NOT_EXECUTED");
            return 0;
        }
        ProbeReceipt receipt = null;
        String stage = "PRIVATE_INPUT_VALIDATION";
        try {
            ServiceAccountCredentials credentials = config.credentials();
            String key = "local/reading/p2-storage-probe/" + UUID.randomUUID() + "/fixture.png";
            receipt = ProbeReceipt.create(config, key);
            stage = "CLIENT_CONSTRUCTION";
            var sdk = StorageOptions.newBuilder().setProjectId(config.project())
                    .setCredentials(credentials).build().getService();
            var properties = new AssetStorageFirebaseProperties(false, config.project(), config.bucket(),
                    config.credential().toString(), "local", null, null, null);
            var adapter = new FirebaseStorageAssetClient(sdk, credentials, properties);
            var storage = new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(adapter)));
            stage = "CREATE_MAY_HAVE_WRITTEN";
            receipt.record(stage);
            var stored = exercise(storage, key, receipt);
            receipt.record("LIVE_GCS_PASS generation=" + stored.version());
            output.println("LIVE_GCS PASS RETAINED_OBJECTS 1");
            return 0;
        } catch (Exception failed) {
            if (receipt != null) {
                try { receipt.record("FAILED_" + stage + " RETAIN_PLANNED_OR_PARTIAL_OWNERSHIP_NO_RETRY"); }
                catch (IOException ignored) { /* The exclusive planned receipt is already retained. */ }
            }
            output.println("LIVE_GCS FAILED_" + stage + " NO_RETRY_NO_CLEANUP");
            return 1;
        }
    }

    static VersionedStorageObject exercise(ReadingObjectStorage storage, String key, ProbeReceipt receipt)
            throws Exception {
        byte[] png = fixture();
        var original = storage.create(PROVIDER, key, "image/png", png, LIMIT, OPERATION_MILLIS);
        verifyOriginal(storage, key, original, png);
        receipt.record("INITIAL_CREATE_AND_FRESH_PINNED_DECODE_PASS generation=" + original.version()
                + " bytes=" + png.length + " sha256=" + ReadingIdentity.sha(png));
        receipt.record("IDENTICAL_CREATE_MAY_HAVE_WRITTEN");
        var reused = storage.create(PROVIDER, key, "image/png", png, LIMIT, OPERATION_MILLIS);
        require(original.equals(reused));
        verifyOriginal(storage, key, original, png);
        receipt.record("IDENTICAL_CONFLICT_PASS");
        byte[] changed = png.clone();
        changed[changed.length - 1] ^= 1;
        receipt.record("SAME_SIZE_CREATE_MAY_HAVE_WRITTEN");
        expectRejected("IMMUTABLE_OBJECT_CONFLICT", () -> storage.create(PROVIDER, key, "image/png", changed, LIMIT, OPERATION_MILLIS));
        verifyOriginal(storage, key, original, png);
        receipt.record("SAME_SIZE_DIFFERENT_BYTES_CONFLICT_PASS");
        receipt.record("WRONG_MIME_CREATE_MAY_HAVE_WRITTEN");
        expectRejected("IMMUTABLE_OBJECT_CONFLICT", () -> storage.create(PROVIDER, key, "application/octet-stream", png, LIMIT, OPERATION_MILLIS));
        verifyOriginal(storage, key, original, png);
        receipt.record("WRONG_MIME_CONFLICT_PASS");
        String impossible = original.version().equals("9223372036854775807") ? "9223372036854775806" : "9223372036854775807";
        expectRejected("PINNED_STORAGE_METADATA_CHANGED", () -> storage.read(PROVIDER,
                new VersionedStorageObject(key, impossible, original.mime(), original.bytes()), LIMIT, OPERATION_MILLIS));
        expectRejected("PINNED_STORAGE_METADATA_CHANGED", () -> storage.read(PROVIDER,
                new VersionedStorageObject(key, original.version(), "image/jpeg", original.bytes()), LIMIT, OPERATION_MILLIS));
        expectRejected("PINNED_STORAGE_METADATA_CHANGED", () -> storage.read(PROVIDER,
                new VersionedStorageObject(key, original.version(), original.mime(), original.bytes() + 1), LIMIT, OPERATION_MILLIS));
        verifyOriginal(storage, key, original, png);
        receipt.record("IMPOSSIBLE_GENERATION_AND_FORGED_METADATA_REJECTED_PASS");
        return original;
    }

    private static void verifyOriginal(ReadingObjectStorage storage, String key, VersionedStorageObject original,
            byte[] expected) throws Exception {
        var fresh = storage.observe(PROVIDER, key, OPERATION_MILLIS);
        require(fresh.equals(original) && fresh.mime().equals("image/png") && fresh.bytes() == expected.length);
        byte[] actual = storage.read(PROVIDER, fresh, LIMIT, OPERATION_MILLIS);
        require(Arrays.equals(actual, expected) && ReadingIdentity.sha(actual).equals(ReadingIdentity.sha(expected)));
        var decoded = ImageIO.read(new ByteArrayInputStream(actual));
        require(decoded != null && decoded.getWidth() == 3 && decoded.getHeight() == 2);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++)
            require((decoded.getRGB(x, y) & 0xffffff) == color(x, y));
    }

    static byte[] fixture() throws IOException {
        var image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 2; y++) for (int x = 0; x < 3; x++) image.setRGB(x, y, color(x, y));
        var output = new ByteArrayOutputStream();
        require(ImageIO.write(image, "png", output));
        byte[] bytes = output.toByteArray();
        require(bytes.length > 0 && bytes.length <= LIMIT);
        return bytes;
    }

    private static int color(int x, int y) { return new int[] {0xff0000, 0x00ff00, 0x0000ff, 0x123456, 0xfedcba, 0x010203}[y * 3 + x]; }
    private static void require(boolean valid) { if (!valid) throw new IllegalStateException("PROBE_ASSERTION_FAILED"); }

    private static void expectRejected(String category, CheckedAction action) throws Exception {
        try { action.run(); }
        catch (IOException | IllegalStateException expected) {
            if (!category.equals(expected.getMessage())) throw expected;
            return;
        }
        throw new IllegalStateException("PROBE_EXPECTED_REJECTION_MISSING");
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
