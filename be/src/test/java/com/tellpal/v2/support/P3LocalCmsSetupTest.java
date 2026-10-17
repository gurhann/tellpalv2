package com.tellpal.v2.support;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class P3LocalCmsSetupTest {
    @TempDir Path directory;

    @Test
    void dryRunDoesNotBootstrapSpringOrReadCredentials() {
        var bytes = new ByteArrayOutputStream();
        assertThat(P3LocalCmsSetup.run(new String[]{}, new PrintStream(bytes))).isZero();
        assertThat(bytes.toString()).contains("ZERO_CREDENTIAL_READS_ZERO_NETWORK_ZERO_DB_WRITES", "OBJECTS_MAX 6");
    }

    @Test
    void rejectsUnknownArgumentsBeforeAnyBootstrap() {
        var bytes = new ByteArrayOutputStream();
        assertThat(P3LocalCmsSetup.run(new String[]{"--bucket", "other"}, new PrintStream(bytes))).isEqualTo(2);
        assertThat(bytes.toString()).isEqualTo("REFUSED_ARGUMENTS" + System.lineSeparator());
    }

    @Test
    void acceptsOnlyUnambiguousLoopbackDatabaseUrls() {
        assertThatCode(() -> P3LocalCmsSetup.validateUrl("jdbc:postgresql://localhost:5432/tellpal_v2")).doesNotThrowAnyException();
        for (String url : List.of("jdbc:postgresql://remote:5432/tellpal_v2", "jdbc:postgresql://localhost/db?host=remote",
                "jdbc:postgresql://localhost@remote/db", "jdbc:postgresql://localhost/db#remote", "jdbc:h2:mem:test")) {
            assertThatThrownBy(() -> P3LocalCmsSetup.validateUrl(url)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void refusesProductionAndMissingScope() {
        var production = new MockEnvironment();
        production.setActiveProfiles("local", "prod");
        assertThatThrownBy(() -> P3LocalCmsSetup.validate(production)).hasMessage("EXPLICIT_LOCAL_ONLY");
        var local = new MockEnvironment();
        local.setActiveProfiles("local");
        local.setProperty("spring.datasource.url", "jdbc:postgresql://localhost/db");
        local.setProperty("tellpal.asset.storage.firebase.bucket-name", "other");
        assertThatThrownBy(() -> P3LocalCmsSetup.validate(local)).hasMessage("BUCKET_OUT_OF_SCOPE");
    }

    @Test
    void explicitOverlayBindsExactSyntheticProfilesAndNormalLocalStartupStaysDisabled() {
        var env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThat(new com.tellpal.v2.content.application.publication.ReadingPublicationProperties(env).enabled()).isFalse();
        env.setProperty("spring.datasource.url", "jdbc:postgresql://localhost/tellpal_v2");
        env.setProperty("tellpal.asset.storage.firebase.bucket-name", P3LocalCmsSetup.BUCKET);
        env.setProperty("tellpal.asset.storage.firebase.path-prefix", "local/p3-cms-12345678-1234-4234-8234-123456789abc");
        env.setProperty("tellpal.asset.processing.poller.enabled", "false");
        env.setProperty("tellpal.asset.reading.test-engine-enabled", "true");
        env.setProperty("tellpal.content.reading-publication.enabled", "true");
        var fields = Map.ofEntries(Map.entry("width", "40"), Map.entry("height", "30"),
                Map.entry("max-source-bytes", "1000000"), Map.entry("max-output-bytes", "1000000"),
                Map.entry("max-pixels", "100000"), Map.entry("parent-memory-bytes", "16000000"),
                Map.entry("heap-mi-b", "64"), Map.entry("direct-mi-b", "8"), Map.entry("process-mi-b", "256"),
                Map.entry("disk-bytes", "2100000"), Map.entry("source-millis", "5000"),
                Map.entry("revision-millis", "30000"), Map.entry("concurrency", "1"),
                Map.entry("encoder-threads", "1"), Map.entry("allow-growth", "true"),
                Map.entry("growth-evidence", P3LocalCmsSetup.EVIDENCE));
        for (String role : List.of("READING_COVER", "READING_DETAIL", "READING_PAGE")) {
            fields.forEach((key, value) -> env.setProperty("tellpal.content.reading-publication.profiles." + role + "." + key, value));
        }
        assertThatCode(() -> P3LocalCmsSetup.validate(env)).doesNotThrowAnyException();
        env.setProperty("tellpal.content.reading-publication.profiles.READING_PAGE.width", "41");
        assertThatThrownBy(() -> P3LocalCmsSetup.validate(env)).hasMessage("SYNTHETIC_PROFILES_REQUIRED");
    }

    @Test
    void fixturePassesRealRgb8InspectorBeforeAnyCloudWrite() throws Exception {
        byte[] bytes = com.tellpal.v2.asset.reading.ReadingFixtures.image(80, 60);
        var decoded = com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector.decode(bytes, P3LocalCmsSetup.profile());
        assertThat(decoded.mime()).isEqualTo("image/png");
        assertThat(decoded.image().getWidth()).isEqualTo(80);
        assertThat(decoded.image().getHeight()).isEqualTo(60);
        assertThat(bytes.length).isLessThanOrEqualTo(1_000_000);
    }

    @Test
    void everyRoleCapturesCanonicalFingerprintWithAsciiMachineEvidence() {
        assertThat(P3LocalCmsSetup.EVIDENCE.chars().allMatch(character -> character <= 127)).isTrue();
        var fingerprints = new java.util.HashSet<String>();
        for (var role : com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.values()) {
            var captured = com.tellpal.v2.asset.domain.reading.ReadingProfiles.capture(role, P3LocalCmsSetup.profile());
            assertThat(captured.fingerprint()).matches("[0-9a-f]{64}");
            assertThat(captured.descriptor()).contains(P3LocalCmsSetup.EVIDENCE);
            fingerprints.add(captured.fingerprint());
        }
        assertThat(fingerprints).hasSize(3);
    }

    @Test
    void exclusiveReceiptPreservesPartialAttemptAndRejectsRerun() throws Exception {
        Path path = directory.resolve("receipt.properties");
        var receipt = P3LocalCmsSetup.Receipt.create(path);
        receipt.record("state", "SOURCE_CREATE_MAY_HAVE_WRITTEN");
        assertThatThrownBy(() -> P3LocalCmsSetup.Receipt.create(path)).isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(java.nio.file.Files.readString(path)).contains("SOURCE_CREATE_MAY_HAVE_WRITTEN");
    }

    @Test
    void existingRecordComparisonDetectsChangedOrRemovedMediaLinks() {
        var before = Map.of("content_localizations", List.of("{id:1,cover:7}"));
        assertThatCode(() -> P3LocalCmsSetup.requirePreserved(before,
                Map.of("content_localizations", List.of("{id:1,cover:7}", "{id:2,cover:8}")))).doesNotThrowAnyException();
        assertThatThrownBy(() -> P3LocalCmsSetup.requirePreserved(before,
                Map.of("content_localizations", List.of("{id:1,cover:8}")))).hasMessage("EXISTING_RECORD_CHANGED");
    }
}
