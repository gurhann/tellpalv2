package com.tellpal.v2.asset.infrastructure.storage;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.tellpal.v2.asset.api.AssetKind;
import static org.assertj.core.api.Assertions.*;

class AssetStorageObjectPathBuilderTest {
    @Test
    void preservesExistingSingleSegmentNormalizationAndOutputShape() {
        for (String prefix : List.of("local", "prod", "test", "custom_env-1")) {
            var builder = builder("  " + prefix.toUpperCase() + "  ");
            assertThat(builder.pathPrefix()).isEqualTo(prefix);
            assertThat(builder.prefixPath("content/demo")).isEqualTo("/" + prefix + "/content/demo");
            assertThat(builder.manualUploadPath(AssetKind.ORIGINAL_IMAGE, "Cover.PNG", Instant.parse("2026-10-17T00:00:00Z")))
                    .startsWith("/" + prefix + "/manual/images/original/2026/10/").endsWith("-cover.png");
        }
    }

    @Test
    void acceptsOnlyP2CompatibleLocalAndTestSubprefixes() {
        for (String prefix : List.of("local/p3-cms-12345678-1234-4234-8234-123456789abc", "test/run_1/nested-2")) {
            var builder = builder(prefix);
            assertThat(builder.pathPrefix()).isEqualTo(prefix);
            assertThat(builder.prefixPath("reading/source")).isEqualTo("/" + prefix + "/reading/source");
            assertThat(builder.requireManagedFirebasePath(prefix + "/fixture.png")).isEqualTo("/" + prefix + "/fixture.png");
        }
    }

    @Test
    void rejectsTraversalEmptySegmentsBackslashAndProductionSubprefixes() {
        for (String prefix : List.of("prod/run", "custom/run", "/local/run", "local/", "local//run",
                "local/../run", "test/./run", "local/a.b", "local/run space", "local/%2e%2e",
                "local\\run", "test/run\\child", "local/run?x=1", "local/run#fragment")) {
            assertThatThrownBy(() -> builder(prefix)).as(prefix).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> builder(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder("  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void managedPathCannotEscapeToParentOrSiblingPrefix() {
        var builder = builder("local/p3-cms-run");
        for (String path : List.of("/local/fixture.png", "/local/p3-cms-run-other/fixture.png", "/prod/p3-cms-run/fixture.png")) {
            assertThatThrownBy(() -> builder.requireManagedFirebasePath(path)).as(path).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static AssetStorageObjectPathBuilder builder(String prefix) {
        return new AssetStorageObjectPathBuilder(new AssetStorageFirebaseProperties(false, null, null, null, prefix, null, null, null));
    }
}
