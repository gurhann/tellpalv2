import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Binding;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Snapshot;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.domain.reading.ReadingWork;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;

// Mevcut derlenmiş P2 sınıflarıyla canonical golden üretir; uygulama başlatmaz.
public class P4ManifestGolden {
    public static void main(String[] args) throws Exception {
        var bindings = List.of(new Binding(Role.READING_COVER, 7, null, 0, 8),
            new Binding(Role.READING_DETAIL, 7, null, 0, 8),
            new Binding(Role.READING_PAGE, 7, 11L, 1, 9));
        var outputs = new ArrayList<ReadingWork.Output>();
        for (var binding : bindings) {
            var key = ReadingIdentity.hash("tellpal.reading.rendition.v1", Map.of(
                "schema", 1, "role", binding.role(), "sourceSha256", "c".repeat(64),
                "profileSha256", "d".repeat(64)));
            outputs.add(new ReadingWork.Output(binding, "c".repeat(64), "d".repeat(64), key,
                StorageProvider.FIREBASE_STORAGE,
                new VersionedStorageObject("test/\"q\"\\slash\b\f\n\r\t\u0001\u007f.png",
                    "v\"\\\t\u001f\u007f", "image/png", 100000), "e".repeat(64), 1200, 800));
        }
        var manifest = new ReadingWork.Manifest("a".repeat(64),
            new Snapshot(71, "tr", bindings, List.of()), outputs);
        Files.writeString(Path.of(args[0]), manifest.canonical());
        System.out.println(manifest.digest());
    }
}
