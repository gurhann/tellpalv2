package com.tellpal.v2.support;

import java.io.PrintStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tellpal.v2.TellPalApplication;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.ReadingObjectStorage;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.content.application.ContentManagementService;
import com.tellpal.v2.content.application.StoryPageManagementService;
import com.tellpal.v2.content.application.ContentManagementCommands.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Yalnız açık çağrıda tek sentetik DRAFT üretir; normal test keşfi buluta erişmez. */
public final class P3LocalCmsSetup {
    static final String BUCKET = "tellpal-ee0dd.appspot.com";
    static final String EVIDENCE = "P3_LOCAL_CMS_SYNTHETIC_RGB8_80X60_TO_40X30_PNG_GROWTH_APPROVED";
    static final List<String> TABLES = List.of("contents", "content_localizations", "story_pages",
            "story_page_localizations", "media_assets", "content_contributors", "content_free_access",
            "story_narrations", "lullaby_playbacks", "lullaby_instruments", "asset_processing");

    private P3LocalCmsSetup() { }

    public static void main(String[] args) {
        System.exit(run(args, System.out));
    }

    static int run(String[] args, PrintStream output) {
        if (args.length == 0 || Arrays.equals(args, new String[]{"--dry-run"})) {
            output.println("DRY_RUN_ZERO_CREDENTIAL_READS_ZERO_NETWORK_ZERO_DB_WRITES");
            output.println("SCOPE " + BUCKET + " local/p3-cms-<UUID> SINGLE_DRAFT_STORY tr ONE_PAGE");
            output.println("RGB8_PNG 80x60 OUTPUT 40x30 OBJECTS_MAX 6 BYTES_EACH_MAX 1000000 TOTAL_MAX 6000000");
            return 0;
        }
        boolean preflight = Arrays.equals(args, new String[]{"--preflight"});
        if (!preflight && !Arrays.equals(args, new String[]{"--execute"})) {
            output.println("REFUSED_ARGUMENTS");
            return 2;
        }
        Receipt receipt = null;
        String stage = "CONFIGURATION";
        try {
            require(System.getenv("SPRING_PROFILES_ACTIVE") == null || "local".equals(System.getenv("SPRING_PROFILES_ACTIVE")), "EXPLICIT_LOCAL_ONLY");
            require(System.getProperty("spring.profiles.active") == null || "local".equals(System.getProperty("spring.profiles.active")), "EXPLICIT_LOCAL_ONLY");
            Path directory = Path.of("var/p3-local-cms").toAbsolutePath().normalize();
            require(directory.toRealPath().equals(directory), "PRIVATE_DIRECTORY_REQUIRED");
            require(Files.getPosixFilePermissions(directory).equals(PosixFilePermissions.fromString("rwx------")), "PRIVATE_DIRECTORY_REQUIRED");
            String overlay = directory.resolve("application-p3.properties").toString();
            require(Files.isRegularFile(Path.of(overlay), LinkOption.NOFOLLOW_LINKS), "OVERLAY_REQUIRED");
            if (!preflight) {
                receipt = Receipt.create(directory.resolve("receipt.properties"));
                receipt.record("state", "PLANNED_NO_RETRY_NO_CLEANUP");
            }
            var app = new SpringApplication(TellPalApplication.class);
            app.setWebApplicationType(WebApplicationType.SERVLET);
            app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> validate(event.getEnvironment()));
            // Hazırlık context'i gerçek web/security bean'lerini kurar; hiçbir scheduler çalıştırmaz.
            app.addInitializers(context -> context.addBeanFactoryPostProcessor(factory -> {
                var registry = (BeanDefinitionRegistry) factory;
                String name = "org.springframework.context.annotation.internalScheduledAnnotationProcessor";
                if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
            }));
            stage = "CONTEXT";
            try (var context = app.run("--spring.profiles.active=local", "--spring.config.additional-location=file:" + overlay,
                    "--spring.main.banner-mode=off", "--spring.flyway.enabled=false", "--server.port=0",
                    "--server.address=127.0.0.1", "--tellpal.security.admin.bootstrap.username=",
                    "--tellpal.security.admin.bootstrap.password=", "--spring.datasource.hikari.read-only=" + preflight)) {
                Environment env = context.getEnvironment();
                validate(env);
                stage = "PROFILE_IDENTITY";
                var profiles = context.getBean(com.tellpal.v2.content.application.publication.ReadingPublicationProperties.class).profiles();
                String identity = context.getBean(ReadingPublicationReadyApi.class).effectiveProfileIdentity(profiles);
                require(identity.matches("[0-9a-f]{64}"), "PROFILE_IDENTITY_REQUIRED");
                String prefix = env.getRequiredProperty("tellpal.asset.storage.firebase.path-prefix");
                var jdbc = context.getBean(JdbcTemplate.class);
                stage = "BASELINE";
                require(jdbc.queryForObject("select count(*) from contents", Long.class) == 91, "EXPECTED_91_EXISTING_CONTENTS");
                for (String table : List.of("reading_source_admissions", "reading_image_jobs", "reading_publications")) {
                    require(jdbc.queryForObject("select count(*) from " + table, Long.class) == 0, "P3_TABLES_NOT_EMPTY");
                }
                Map<String, List<String>> baseline = snapshot(jdbc);
                if (preflight) {
                    output.println("PREFLIGHT_PASS_REAL_CONTEXT_EXACT_PROFILES_EFFECTIVE_IDENTITY_91_CONTENTS_READONLY_BASELINE_NO_CLOUD");
                    return 0;
                }
                receipt.record("prefix", prefix);
                receipt.record("bucket", BUCKET);
                receipt.record("databaseUrl", env.getRequiredProperty("spring.datasource.url"));
                Path before = directory.resolve("before.properties");
                storeSnapshot(before, baseline);
                receipt.record("baseline", sha(Files.readAllBytes(before)));
                var assets = context.getBean(AssetRegistryApi.class);
                var storage = context.getBean(ReadingObjectStorage.class);
                byte[] bytes = ReadingFixtures.image(80, 60);
                require(bytes.length <= 1_000_000, "FIXTURE_LIMIT");
                Files.write(directory.resolve("fixture.png"), bytes, StandardOpenOption.CREATE_NEW);
                String path = prefix + "/fixture.png";
                receipt.record("sourcePath", path);
                receipt.record("sourceSha256", sha(bytes));
                stage = "SOURCE_CREATE_MAY_HAVE_WRITTEN";
                receipt.record("state", stage);
                var stored = storage.create(StorageProvider.FIREBASE_STORAGE, path, "image/png", bytes, 1_000_000, 5000);
                receipt.record("sourceVersion", stored.version());
                var original = assets.register(new RegisterMediaAssetCommand(AssetStorageProvider.FIREBASE_STORAGE,
                        path, AssetKind.ORIGINAL_IMAGE, "image/png", (long) bytes.length, sha(bytes)));
                receipt.record("originalAssetId", original.assetId().toString());
                stage = "ADMISSION_OR_SEED_MAY_HAVE_WRITTEN";
                receipt.record("state", stage);
                admitAndSeed(context.getBean(ReadingSourceAdmissionApi.class), original.assetId(),
                        context.getBean(ContentManagementService.class), context.getBean(StoryPageManagementService.class),
                        assets, prefix, receipt);
                Map<String, List<String>> after = snapshot(jdbc);
                requirePreserved(baseline, after);
                storeSnapshot(directory.resolve("after.properties"), after);
                require(jdbc.queryForObject("select count(*) from contents", Long.class) == 92, "EXPECTED_SINGLE_ADDITION");
                receipt.record("existingRecords", "PRESERVED_91_CONTENTS_AND_MEDIA_BINDINGS");
                receipt.record("state", "DRAFT_READY_PUBLISH_IN_CMS");
                output.println("DRAFT_READY_PUBLISH_IN_CMS EXISTING_91_PRESERVED NO_RETRY_NO_CLEANUP");
            }
            return 0;
        } catch (Exception failure) {
            if (receipt != null) {
                try { receipt.record("state", "FAILED_" + stage + "_NO_RETRY_NO_CLEANUP"); }
                catch (Exception ignored) { /* İlk özel makbuz ve DB sahiplik kanıtı korunur. */ }
            }
            output.println("REFUSED_OR_FAILED_" + stage + "_NO_RETRY_NO_CLEANUP");
            return 2;
        }
    }

    static ReadingImagePreparationApi.TestProfile profile() {
        var p = ReadingFixtures.profile();
        return new ReadingImagePreparationApi.TestProfile(p.width(), p.height(), p.maxSourceBytes(), p.maxOutputBytes(),
                p.maxPixels(), p.parentMemoryBytes(), p.heapMiB(), p.directMiB(), p.processMiB(), p.diskBytes(),
                p.sourceMillis(), p.revisionMillis(), p.concurrency(), p.encoderThreads(), true, EVIDENCE);
    }

    /** Yeni admitted kimliğini yalnız oluşturulan demo kapağına ve tek sayfasına bağlar. */
    static Seed admitAndSeed(ReadingSourceAdmissionApi admissions, long originalAssetId,
            ContentManagementService contents, StoryPageManagementService pages,
            AssetRegistryApi assets, String prefix, Receipt receipt) throws Exception {
        receipt.record("state", "ADMISSION_MAY_HAVE_WRITTEN");
        var admission = admissions.admit(originalAssetId, profile());
        receipt.record("admissionId", admission.id().toString());
        receipt.record("admittedAssetId", Long.toString(admission.assetId()));
        receipt.record("state", "SEED_PARTIAL");
        long contentId = seed(contents, pages, assets, prefix, admission.assetId(), receipt);
        return new Seed(contentId, admission);
    }

    record Seed(long contentId, ReadingSourceAdmissionApi.Admission admission) { }

    private static long seed(ContentManagementService contents, StoryPageManagementService pages,
            AssetRegistryApi assets, String prefix, long imageId, Receipt receipt) throws Exception {
        var audio = assets.register(new RegisterMediaAssetCommand(AssetStorageProvider.LOCAL_STUB,
                prefix + "/audio-record-only.mp3", AssetKind.ORIGINAL_AUDIO, "audio/mpeg", 0L, null));
        receipt.record("audioAssetId", audio.assetId().toString());
        String externalKey = prefix.substring("local/".length());
        var content = contents.createContent(new CreateContentCommand(ContentType.STORY, externalKey, 3, true));
        Long id = content.contentId();
        receipt.record("contentId", id.toString());
        receipt.record("externalKey", externalKey);
        var tr = LanguageCode.from("tr");
        contents.createLocalization(new CreateContentLocalizationCommand(id, tr, "P3 yerel görsel denemesi",
                "Tek sayfalı sentetik görsel hazırlama denemesi. Ses yalnız test kaydıdır.", null,
                imageId, null, 0, LocalizationStatus.DRAFT, ProcessingStatus.PENDING, null));
        var page = pages.addStoryPage(new AddStoryPageCommand(id, null));
        receipt.record("pageNumber", Integer.toString(page.pageNumber()));
        pages.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(id, 1, tr,
                "Renkler bir araya geldi. Bu sayfa yalnız yerel P3 denemesi içindir.", audio.assetId(), imageId));
        return id;
    }

    static void validate(Environment env) {
        require(Set.of(env.getActiveProfiles()).equals(Set.of("local")), "EXPLICIT_LOCAL_ONLY");
        validateUrl(env.getRequiredProperty("spring.datasource.url"));
        require(BUCKET.equals(env.getRequiredProperty("tellpal.asset.storage.firebase.bucket-name")), "BUCKET_OUT_OF_SCOPE");
        String prefix = env.getRequiredProperty("tellpal.asset.storage.firebase.path-prefix");
        require(prefix.matches("local/p3-cms-[0-9a-f-]{36}"), "PREFIX_OUT_OF_SCOPE");
        UUID id = UUID.fromString(prefix.substring("local/p3-cms-".length()));
        require(id.version() == 4 && ("local/p3-cms-" + id).equals(prefix), "PREFIX_OUT_OF_SCOPE");
        require(!env.getProperty("tellpal.asset.storage.firebase.fake-client-enabled", Boolean.class, false), "REAL_SDK_REQUIRED");
        require(!env.getProperty("tellpal.asset.processing.poller.enabled", Boolean.class, true), "LEGACY_POLLER_MUST_BE_DISABLED");
        require(env.getProperty("tellpal.asset.reading.test-engine-enabled", Boolean.class, false), "TEST_ENGINE_REQUIRED");
        require(env.getProperty("tellpal.content.reading-publication.enabled", Boolean.class, false), "P3_REQUIRED");
        var profiles = Binder.get(env).bind("tellpal.content.reading-publication.profiles",
                Bindable.mapOf(ReadingImagePreparationApi.Role.class, ReadingImagePreparationApi.TestProfile.class)).orElse(Map.of());
        require(profiles.equals(Map.of(ReadingImagePreparationApi.Role.READING_COVER, profile(),
                ReadingImagePreparationApi.Role.READING_DETAIL, profile(), ReadingImagePreparationApi.Role.READING_PAGE, profile())), "SYNTHETIC_PROFILES_REQUIRED");
    }

    static void validateUrl(String jdbc) {
        require(jdbc.startsWith("jdbc:postgresql://"), "LOCAL_DB_REQUIRED");
        URI uri = URI.create(jdbc.substring(5));
        require(Set.of("localhost", "127.0.0.1", "[::1]").contains(Objects.toString(uri.getHost(), ""))
                && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getFragment() == null
                && uri.getPath() != null && uri.getPath().matches("/[A-Za-z0-9_]+"), "LOCAL_DB_REQUIRED");
    }

    static Map<String, List<String>> snapshot(JdbcTemplate jdbc) {
        var result = new TreeMap<String, List<String>>();
        for (String table : TABLES) {
            result.put(table, jdbc.queryForList("select row_to_json(t)::text from " + table + " t order by row_to_json(t)::text", String.class));
        }
        return result;
    }

    static void requirePreserved(Map<String, List<String>> before, Map<String, List<String>> after) {
        before.forEach((table, rows) -> {
            var remaining = new ArrayList<>(after.getOrDefault(table, List.of()));
            rows.forEach(row -> require(remaining.remove(row), "EXISTING_RECORD_CHANGED"));
        });
    }

    private static void storeSnapshot(Path path, Map<String, List<String>> snapshot) throws Exception {
        var properties = new Properties();
        snapshot.forEach((table, rows) -> {
            for (int i = 0; i < rows.size(); i++) properties.setProperty(table + "." + i, rows.get(i));
        });
        try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            properties.store(output, "P3 mevcut kayıt karşılaştırması; özel yerel kanıt");
        }
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalArgumentException(code);
    }

    static final class Receipt {
        private final Path path;
        private Receipt(Path path) { this.path = path; }
        static Receipt create(Path path) throws Exception {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            return new Receipt(path);
        }
        void record(String key, String value) throws Exception {
            require(key.matches("[A-Za-z0-9]+") && !value.contains("\n") && !value.contains("\r"), "INVALID_RECEIPT");
            try (var channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                ByteBuffer data = java.nio.charset.StandardCharsets.UTF_8.encode(key + "=" + value + "\n");
                while (data.hasRemaining()) channel.write(data);
                channel.force(true);
            }
        }
    }
}
