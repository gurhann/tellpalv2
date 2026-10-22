package com.tellpal.v2.asset.infrastructure.media.reading;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;

/** Supervisor/parser yaması sonrası eski üretim kimliğini koruyarak 41 çifti yeniden okur. */
@EnabledIfSystemProperty(named="package1.patch.corpus",matches=".+")
class Package1PatchRereadTest {
    @Test void patchedParserRereadsAll41AndEncodingRawCoreIdentityRemainsUnchanged() throws Exception {
        var json=new ObjectMapper();Path corpus=Path.of(System.getProperty("package1.patch.corpus"));
        var produced=json.readTree(Files.readAllBytes(corpus.resolve("result.json")));
        var p=json.treeToValue(produced.path("profiles").path("limits"),TestProfile.class);
        var profile=ReadingProfiles.capture(Role.READING_PAGE,p);
        var before=json.readTree(produced.path("profiles").path("descriptor").asText());
        var after=json.readTree(profile.descriptor());
        var unchanged=new TreeMap<String,String>();
        for(String name:List.of("ReadingPreservationEncoder","ReadingPreservationRawDecoder","ReadingPreservation","ReadingPreservationContainers")) {
            String type="com.tellpal.v2.asset.infrastructure.media.reading."+name;
            String oldHash=before.path("runtimeAndEngineBinaries").path(type).asText();
            assertThat(after.path("runtimeAndEngineBinaries").path(type).asText()).as(type).isEqualTo(oldHash);
            unchanged.put(type,oldHash);
        }
        for(var row:before.path("runtimeAndEngineBinaries").properties()) if(row.getKey().endsWith(".jar"))
            assertThat(after.path("runtimeAndEngineBinaries").path(row.getKey())).isEqualTo(row.getValue());
        assertThat(after.path("encoder")).isEqualTo(before.path("encoder"));
        Path report=Path.of(System.getProperty("package1.patch.sources"));
        var byId=new HashMap<Long,com.fasterxml.jackson.databind.JsonNode>();
        json.readTree(Files.readAllBytes(report)).path("sources").forEach(row->byId.put(row.path("assetId").asLong(),row));
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
        json.writerWithDefaultPrettyPrinter().writeValue(Path.of(System.getProperty("package1.patch.output")).toFile(),
                Map.of("status","PASS","encodingProfileFingerprint",produced.path("profiles").path("fingerprint").asText(),
                        "rereadProfile",profile,"unchangedEncodingRawCoreClasses",unchanged,"newEncoding",false,
                        "originalEncodingIdentityPreserved",true,"sources",results));
    }
}
