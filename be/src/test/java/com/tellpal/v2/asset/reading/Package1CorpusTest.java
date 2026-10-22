package com.tellpal.v2.asset.infrastructure.media.reading;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.asset.application.reading.ReadingImagePreparationService;

/** 41 pinli kaynaktan yeni ana motor çıktısı üretir; eski adayları yalnız koruma için okur. */
@EnabledIfSystemProperty(named="package1.corpus.report", matches=".+")
class Package1CorpusTest {
    @Test void all41SourcesProduce17ExactCopiesAnd24NewWebpWithIndependentStoredProof() throws Exception {
        var json = new ObjectMapper();
        Path reportPath = Path.of(System.getProperty("package1.corpus.report"));
        Path baselinePath = Path.of(System.getProperty("package1.corpus.baseline"));
        Path outputPath = Path.of(System.getProperty("package1.corpus.output")); Files.createDirectories(outputPath);
        var baseline = json.readTree(Files.readAllBytes(baselinePath));
        assertThat(ReadingIdentity.sha(Files.readAllBytes(reportPath))).isEqualTo(baseline.path("p5c").path(reportPath.toString()).asText());
        protectedFiles(json, baseline);
        var rows = json.readTree(Files.readAllBytes(reportPath)).path("sources"); assertThat(rows.size()).isEqualTo(41);
        var p = new TestProfile(1712,2778,16L<<20,16L<<20,5_000_000,256L<<20,256,32,512,128L<<20,
                60_000,1_800_000,1,1,true,"Package1 local corpus only; equal/growing output explicitly admitted; not production",FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
        var evidence = new ArrayList<Map<String,Object>>(); int jpeg = 0, webp = 0;
        var disk = new ReadingFixtures.DiskStorage(outputPath.resolve("stored")); var storage = disk.storage();
        for (var row : rows) {
            long id = row.path("assetId").asLong(); Path sourcePath = reportPath.getParent().resolve(row.path("sourceFile").asText());
            byte[] original = Files.readAllBytes(sourcePath); assertThat(ReadingIdentity.sha(original)).isEqualTo(row.path("sourceSha256").asText());
            assertThat(original.length).isEqualTo(row.path("sourceBytes").asInt());
            assertThat(baseline.path("originals").properties().stream().anyMatch(entry -> entry.getValue().asText().equals(ReadingIdentity.sha(original)))).isTrue();
            long started = System.nanoTime(); var process = new ReadingImageProcess(); byte[] result = process.convert(original,p);
            String inputMime = ReadingPreservation.mime(original), mime = ReadingPreservation.mime(result);
            String filename = id + (mime.equals("image/jpeg") ? ".jpg" : ".webp"); Files.write(outputPath.resolve(filename), result, StandardOpenOption.CREATE_NEW);
            var raw = ReadingPreservationRawDecoder.decode(original,inputMime,p);
            var independentlyRead = ReadingPreservationRawDecoder.decode(Files.readAllBytes(outputPath.resolve(filename)),mime,p);
            ReadingPreservationRawDecoder.same(raw,independentlyRead);
            var provider = StorageProvider.FIREBASE_STORAGE;
            var sourceObject = storage.create(provider,"source/"+id,inputMime,original,p.maxSourceBytes());
            var object = storage.create(provider,"output/"+id,mime,result,p.maxOutputBytes());
            var binding = new Binding(Role.READING_PAGE,1,1L,1,id);
            var source = new ReadingWork.Source(binding,provider,sourceObject,sourceObject,ReadingIdentity.sha(original),raw.width(),raw.height());
            var profile = ReadingProfiles.capture(Role.READING_PAGE,p);
            var output = new ReadingWork.Output(binding,source.sha256(),profile.fingerprint(),ReadingWork.rendition(source,profile),provider,object,ReadingIdentity.sha(result),raw.width(),raw.height());
            ReadingImagePreparationService.verifyOutput(storage,source,output,p,ReadingDeadline.start(p.revisionMillis()));
            if(inputMime.equals("image/jpeg")) { assertThat(result).isEqualTo(original); jpeg++; } else { assertThat(mime).isEqualTo("image/webp"); webp++; }
            assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            evidence.add(Map.ofEntries(Map.entry("assetId",id),Map.entry("sourceSha256",source.sha256()),Map.entry("outputSha256",output.sha256()),
                    Map.entry("sourceBytes",original.length),Map.entry("outputBytes",result.length),Map.entry("mime",mime),Map.entry("width",raw.width()),Map.entry("height",raw.height()),
                    Map.entry("rgbSha256",ReadingIdentity.sha(raw.rgb())),Map.entry("metadata",raw.metadata().summary()),Map.entry("wallMillis",(System.nanoTime()-started)/1_000_000)));
        }
        assertThat(jpeg).isEqualTo(17); assertThat(webp).isEqualTo(24); protectedFiles(json,baseline);
        json.writerWithDefaultPrettyPrinter().writeValue(outputPath.resolve("result.json").toFile(),Map.of("copies",jpeg,"newWebp",webp,"profiles",ReadingProfiles.capture(Role.READING_PAGE,p),"sources",evidence,"protectedInputsUnchanged",true));
    }
    private static void protectedFiles(ObjectMapper json, com.fasterxml.jackson.databind.JsonNode baseline) throws Exception {
        for (String group : List.of("originals","p5c","p5fEvidence")) for (var entry : baseline.path(group).properties()) {
            Path path=Path.of(entry.getKey()); if(!path.isAbsolute()) path=Path.of(System.getProperty("package1.corpus.repo", "..")).resolve(path);
            assertThat(ReadingIdentity.sha(Files.readAllBytes(path))).as(group+":"+entry.getKey()).isEqualTo(entry.getValue().asText());
        }
    }
}
