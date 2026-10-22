package com.tellpal.v2.asset.infrastructure.media.reading;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;

/** Yalnız config annotation düzeltmesinden sonra eski üretim kimliğini koruyarak yeniden doğrular. */
@EnabledIfSystemProperty(named="package1.reread.corpus",matches=".+")
class Package1CorpusRereadTest {
    @Test void finalClassesIndependentlyRereadAll41AndOnlyConstructorAnnotationIdentityChanged() throws Exception {
        var json=new ObjectMapper();Path corpus=Path.of(System.getProperty("package1.reread.corpus"));
        var produced=json.readTree(Files.readAllBytes(corpus.resolve("result.json")));
        var p=json.treeToValue(produced.path("profiles").path("limits"),TestProfile.class);
        var profile=ReadingProfiles.capture(Role.READING_PAGE,p);
        var before=json.readTree(produced.path("profiles").path("descriptor").asText());
        var after=json.readTree(profile.descriptor());
        String type=TestProfile.class.getName();
        assertThat(before.path("runtimeAndEngineBinaries").path(type).asText()).isNotEqualTo(after.path("runtimeAndEngineBinaries").path(type).asText());
        String oldProfile=produced.path("profiles").path("fingerprint").asText();
        assertThat(profile.fingerprint()).isNotEqualTo(oldProfile);
        ((com.fasterxml.jackson.databind.node.ObjectNode)before.path("runtimeAndEngineBinaries")).remove(type);
        ((com.fasterxml.jackson.databind.node.ObjectNode)after.path("runtimeAndEngineBinaries")).remove(type);
        assertThat(after).isEqualTo(before);
        Path report=Path.of(System.getProperty("package1.reread.sources"));
        var sources=json.readTree(Files.readAllBytes(report)).path("sources");
        var byId=new HashMap<Long,com.fasterxml.jackson.databind.JsonNode>();sources.forEach(row->byId.put(row.path("assetId").asLong(),row));
        var results=new ArrayList<Map<String,Object>>();
        for(var row:produced.path("sources")) {
            long id=row.path("assetId").asLong();var input=byId.get(id);
            byte[] source=Files.readAllBytes(report.getParent().resolve(input.path("sourceFile").asText()));
            byte[] candidate=Files.readAllBytes(corpus.resolve(id+(row.path("mime").asText().equals("image/jpeg")?".jpg":".webp")));
            assertThat(ReadingIdentity.sha(source)).isEqualTo(row.path("sourceSha256").asText());
            assertThat(ReadingIdentity.sha(candidate)).isEqualTo(row.path("outputSha256").asText());
            ReadingPreservation.verify(source,candidate,p);
            results.add(Map.of("assetId",id,"sourceSha256",ReadingIdentity.sha(source),"outputSha256",ReadingIdentity.sha(candidate),"fullRgbMetadataVerified",true));
        }
        assertThat(results).hasSize(41);
        json.writerWithDefaultPrettyPrinter().writeValue(Path.of(System.getProperty("package1.reread.output")).toFile(),
                Map.of("status","PASS","encodingProfileFingerprint",oldProfile,"rereadProfile",profile,"onlyChangedRuntimeClass",type,
                        "newEncoding",false,"originalEncodingIdentityPreserved",true,"sources",results));
    }
}
