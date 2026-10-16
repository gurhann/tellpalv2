package com.tellpal.v2.asset.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ReadingStorageLiveProbeTest {
    private static final Path REPOSITORY = Path.of("..").toAbsolutePath().normalize();

    @Test
    void dryRunDoesNotOpenEvenMissingCredentialOrReceiptParents() {
        var output = new ByteArrayOutputStream();
        assertEquals(0, ReadingStorageLiveProbe.run(arguments(false, Path.of("/nonexistent/private/secret.json"),
                Path.of("/nonexistent/private/receipt")), new PrintStream(output)));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK"));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("LIVE_GCS NOT_EXECUTED"));
    }

    @Test
    void unknownDuplicateRelativeAndTraversalTargetOptionsAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> ProbeConfiguration.parse(new String[] {"--object-key", "prod/anything"}));
        assertThrows(IllegalArgumentException.class, () -> ProbeConfiguration.parse(new String[] {"--project", "probe-local", "--project", "other"}));
        var relative = arguments(false, Path.of("relative.json"), Path.of("/private/tmp/receipt"));
        assertThrows(IllegalArgumentException.class, () -> ProbeConfiguration.parse(relative));
        var args = arguments(false, Path.of("/private/tmp/credential"), Path.of("/private/tmp/receipt"));
        args[3] = "gs://prod/bucket";
        final var invalidBucket = args;
        assertThrows(IllegalArgumentException.class, () -> ProbeConfiguration.parse(invalidBucket));
    }

    @Test
    void invalidLivePrivateInputHasFiniteSanitizedFailureAndNoReceipt() throws Exception {
        withPrivateDirectory(directory -> {
            Path credential = directory.resolve("credential.json");
            Files.writeString(credential, "secret-private-key token_uri https://untrusted.invalid");
            Files.setPosixFilePermissions(credential, PosixFilePermissions.fromString("rw-r--r--"));
            var output = new ByteArrayOutputStream();
            var receipt = directory.resolve("receipt");
            assertEquals(1, ReadingStorageLiveProbe.run(arguments(true, credential, receipt), new PrintStream(output)));
            assertEquals("LIVE_GCS FAILED_PRIVATE_INPUT_VALIDATION NO_RETRY_NO_CLEANUP\n", output.toString(StandardCharsets.UTF_8));
            assertFalse(Files.exists(receipt));
        });
    }

    @Test
    void rejectsWrongCredentialTypeProjectTokenEndpointAndEmailBindingBeforeKeyParsing() throws Exception {
        withPrivateDirectory(directory -> {
            Path file = directory.resolve("credential.json");
            var config = ProbeConfiguration.parse(arguments(true, file, directory.resolve("receipt")));
            for (String json : List.of("{}", "{\"type\":\"authorized_user\"}",
                    credentialJson("another-project", "probe@probe-local.iam.gserviceaccount.com", "https://oauth2.googleapis.com/token"),
                    credentialJson("probe-local", "probe@another-project.iam.gserviceaccount.com", "https://oauth2.googleapis.com/token"),
                    credentialJson("probe-local", "probe@probe-local.iam.gserviceaccount.com", "http://127.0.0.1/token"))) {
                Files.writeString(file, json);
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
                var rejected = assertThrows(java.io.IOException.class, config::credentials);
                assertEquals("CREDENTIAL_BINDING", rejected.getMessage());
            }
        });
    }

    @Test
    void receiptCreationIsExclusivePrivateAndPreservesPartialOwnership() throws Exception {
        withPrivateDirectory(directory -> {
            Path target = directory.resolve("receipt");
            var config = ProbeConfiguration.parse(arguments(false, directory.resolve("unused"), target));
            var receipt = ProbeReceipt.create(config, "local/reading/p2-storage-probe/test/fixture.png");
            receipt.record("CREATE_MAY_HAVE_WRITTEN");
            assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(target));
            assertThrows(java.nio.file.FileAlreadyExistsException.class,
                    () -> ProbeReceipt.create(config, "local/reading/p2-storage-probe/other/fixture.png"));
            assertTrue(Files.readString(target).contains("CREATE_MAY_HAVE_WRITTEN"));
            assertTrue(Files.readString(target).contains("WORKER " + ReadingStorageLiveProbe.class.getName()));
            Path link = directory.resolve("link");
            Files.createSymbolicLink(link, target);
            assertThrows(java.io.IOException.class, () -> config.privateFile(link));
        });
    }

    @Test
    @Timeout(20)
    void actualAdapterAndWrapperIndependentlyEnforceConflictEqualityAndPinnedMetadata() throws Exception {
        withPrivateDirectory(directory -> {
            try (var endpoint = new ReadingStorageTransportTest.Endpoint(ReadingStorageTransportTest.Mode.NORMAL)) {
                var config = ProbeConfiguration.parse(arguments(false, directory.resolve("unused"), directory.resolve("receipt")));
                var receipt = ProbeReceipt.create(config, "fixture.png");
                var storage = new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(endpoint.adapter())));
                var stored = ReadingStorageLiveProbe.exercise(storage, "fixture.png", receipt);
                assertEquals("17", stored.version());
                assertEquals(4, endpoint.creates.get());
                assertEquals(4, endpoint.queries.stream().filter(query -> query.contains("ifGenerationMatch=0")).count());
                assertTrue(endpoint.queries.stream().anyMatch(query -> query.contains("generation=9223372036854775807")));
                assertTrue(endpoint.queries.stream().filter(query -> query.contains("alt=media"))
                        .allMatch(query -> query.contains("generation=17") && query.contains("ifGenerationMatch=17")));
                assertTrue(Files.readString(config.receipt()).contains("SAME_SIZE_DIFFERENT_BYTES_CONFLICT_PASS"));
                assertArrayEquals(ReadingStorageLiveProbe.fixture(), storage.read(ReadingStorageLiveProbe.PROVIDER,
                        stored, ReadingStorageLiveProbe.LIMIT, 3_000));
            }
        });
    }

    @Test
    void ambiguousCreateFailureStopsWithoutLaterCreateAttemptsOrNewKey() throws Exception {
        withPrivateDirectory(directory -> {
            try (var endpoint = new ReadingStorageTransportTest.Endpoint(ReadingStorageTransportTest.Mode.AMBIGUOUS_CREATE_FAILURE)) {
                var config = ProbeConfiguration.parse(arguments(false, directory.resolve("unused"), directory.resolve("receipt")));
                var receipt = ProbeReceipt.create(config, "fixture.png");
                var storage = new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(endpoint.adapter())));
                var failure = assertThrows(com.google.cloud.storage.StorageException.class,
                        () -> ReadingStorageLiveProbe.exercise(storage, "fixture.png", receipt));
                assertEquals(503, failure.getCode());
                assertEquals(2, endpoint.creates.get());
                assertTrue(Files.readString(config.receipt()).contains("IDENTICAL_CREATE_MAY_HAVE_WRITTEN"));
                assertFalse(Files.readString(config.receipt()).contains("SAME_SIZE_CREATE_MAY_HAVE_WRITTEN"));
            }
        });
    }

    private static String credentialJson(String project, String email, String token) {
        return "{\"type\":\"service_account\",\"project_id\":\"" + project + "\",\"client_email\":\"" + email + "\",\"token_uri\":\"" + token + "\"}";
    }

    private static String[] arguments(boolean live, Path credential, Path receipt) {
        var args = new java.util.ArrayList<>(List.of("--project", "probe-local", "--bucket", "probe-bucket",
                "--credential", credential.toString(), "--receipt", receipt.toString(), "--repository", REPOSITORY.toString()));
        if (live) args.add("--execute-live");
        return args.toArray(String[]::new);
    }

    private static void withPrivateDirectory(DirectoryAction action) throws Exception {
        Path directory = Files.createTempDirectory("p2-storage-probe-test-").toRealPath();
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        try { action.run(directory); }
        finally {
            try (var files = Files.list(directory)) { for (Path file : files.toList()) Files.delete(file); }
            Files.delete(directory);
        }
    }

    @FunctionalInterface private interface DirectoryAction { void run(Path directory) throws Exception; }
}
