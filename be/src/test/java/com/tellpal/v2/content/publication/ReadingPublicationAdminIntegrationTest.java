package com.tellpal.v2.content.publication;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.convention.TestBean;
import com.fasterxml.jackson.databind.JsonNode;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.reading.ReadingFixtures;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.content.application.publication.ReadingPublicationProperties;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

/** Gerçek HTTP yükleme/yayın hattı; yalnız storage leaf yerel disktir. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "tellpal.security.admin.cors.allowed-origins=http://127.0.0.1:4177",
    "tellpal.asset.reading.test-engine-enabled=true","tellpal.asset.reading.poll-delay=250",
    "tellpal.content.reading-publication.poll-delay=250",
    "tellpal.content.reading-publication.preview.run-id=cms-publish-test",
    "tellpal.content.reading-publication.preview.max-encoded-bytes=1000000",
    "tellpal.content.reading-publication.preview.max-pixels=100000",
    "tellpal.content.reading-publication.preview.max-memory-bytes=16000000",
    "tellpal.content.reading-publication.preview.deadline-millis=10000",
    "tellpal.content.reading-publication.preview.max-total-operations=1000",
    "tellpal.content.reading-publication.preview.max-total-bytes=100000000"})
class ReadingPublicationAdminIntegrationTest extends AdminApiIntegrationTestSupport {
    @TestBean(methodName="localStorage") AssetStorageClient firebaseAssetStorageClient;
    @Autowired ReadingPublicationProperties config;
    @Autowired com.tellpal.v2.content.application.ContentPublicationService publicationCommands;
    @Autowired com.tellpal.v2.content.application.ContentManagementService contentCommands;
    @LocalServerPort int port;
    String token;
    static LocalDisk localStorage() throws IOException {return new LocalDisk(Files.createTempDirectory("tellpal-cms-publish-storage-"));}
    @BeforeEach void setup() throws Exception {
        config.setEnabled(false);
        jdbcTemplate.execute("truncate reading_publications,reading_source_admissions,reading_image_jobs,contents,media_assets,admin_users cascade");
        token=authenticateAdmin();
        var p=ReadingFixtures.profile();
        var preserve=new TestProfile(100,100,p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,"Yerel sentetik kabul; JPEG esit boyut ve lossless WebP",FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
        config.setProfiles(Map.of(Role.READING_COVER,preserve,Role.READING_DETAIL,preserve,Role.READING_PAGE,preserve));config.setEnabled(true);
    }
    @AfterEach void disable(){config.setEnabled(false);}
    JsonNode json(String method,String path,String body) throws Exception {
        var request=switch(method){case "POST"->post(path);case "PUT"->put(path);default->get(path);};
        if(body!=null)request.contentType("application/json").content(body);
        return readPayload(mockMvc.perform(request.header("Authorization","Bearer "+token)).andExpect(status().is2xxSuccessful()).andReturn());
    }
    long upload(String name,String mime,byte[] bytes) throws Exception {
        return readPayload(mockMvc.perform(multipart("/api/admin/media/uploads").file(new MockMultipartFile("file",name,mime,bytes))
                .param("kind",mime.startsWith("image/")?"ORIGINAL_IMAGE":"ORIGINAL_AUDIO").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andReturn()).get("assetId").asLong();
    }
    long draft() throws Exception {
        long id=json("POST","/api/admin/contents","{\"type\":\"STORY\",\"externalKey\":\"cms-"+UUID.randomUUID()+"\",\"active\":true,\"ageRange\":5}").get("contentId").asLong();
        json("POST","/api/admin/contents/"+id+"/localizations/tr","{\"title\":\"Yerel test hikayesi\",\"description\":\"Sentetik kabul\",\"durationMinutes\":5,\"status\":\"DRAFT\",\"processingStatus\":\"COMPLETED\"}");
        json("POST","/api/admin/contents/"+id+"/story-pages","{}");
        long audio=upload("fixture.wav","audio/wav",new byte[]{1,2,3});
        long placeholder=upload("initial-page.png","image/png",ReadingFixtures.quadrants("png"));
        json("PUT","/api/admin/contents/"+id+"/story-pages/1/localizations/tr","{\"bodyText\":\"Yerel sayfa\",\"audioMediaId\":"+audio+",\"illustrationMediaId\":"+placeholder+"}");
        return id;
    }
    @Test void authenticatedCapabilityIsExplicitDisabledByDefaultAndLanguageValidationIsFinite() throws Exception {
        config.setEnabled(false);
        mockMvc.perform(get("/api/admin/contents/1/reading-publication?languageCode=tr")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/contents/1/reading-publication?languageCode=tr").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false)).andExpect(jsonPath("$.state").value("DISABLED"));
        mockMvc.perform(post("/api/admin/contents/1/reading-publication/retry?languageCode=tr").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("DISABLED"));
        mockMvc.perform(get("/api/admin/contents/1/reading-publication?languageCode=TR").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        mockMvc.perform(get(preview(1)).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        config.setEnabled(true);long id=draft();
        mockMvc.perform(get("/api/admin/contents/"+id+"/reading-publication?languageCode=tr").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("NOT_REQUESTED"));
    }
    @Test void normalMultipartUploadSaveAndSinglePublishProduceVerifiedPreviewAndPreserveOriginals() throws Exception {
        com.tellpal.v2.asset.reading.ReadingNativeTestTools.require();
        long id=draft();byte[] jpeg=ReadingFixtures.quadrants("jpeg"),png=ReadingFixtures.quadrants("png");
        long cover=upload("cover.jpg","image/jpeg",jpeg),page=upload("page.png","image/png",png);
        json("PUT","/api/admin/contents/"+id+"/localizations/tr","{\"title\":\"Yerel test\",\"description\":\"Sentetik kabul\",\"coverMediaId\":"+cover+",\"durationMinutes\":5,\"status\":\"DRAFT\",\"processingStatus\":\"COMPLETED\"}");
        long audio=jdbcTemplate.queryForObject("select audio_media_id from story_page_localizations where language_code='tr'",Long.class);
        json("PUT","/api/admin/contents/"+id+"/story-pages/1/localizations/tr","{\"bodyText\":\"Yerel sayfa\",\"audioMediaId\":"+audio+",\"illustrationMediaId\":"+page+"}");
        assertThat(mockMvc.perform(get(preview(id)).header("Authorization","Bearer "+token)).andReturn().getResponse().getStatus()).isEqualTo(409);
        json("POST","/api/admin/contents/"+id+"/localizations/tr/publish","{}");
        JsonNode status=null;
        for(int i=0;i<200;i++){status=json("GET","/api/admin/contents/"+id+"/reading-publication?languageCode=tr",null);if(status.path("state").asText().equals("LIVE"))break;Thread.sleep(100);}
        assertThat(status.path("state").asText()).isEqualTo("LIVE");
        var result=mockMvc.perform(get(preview(id)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","private,no-store")).andReturn();
        String body=result.getResponse().getContentAsString();assertThat(body).doesNotContain("objectKey","objectPath","provider","generation\"","https:");
        var manifest=readPayload(result);var identity=manifest.path("identity");
        for(var image:manifest.path("images")) {
            var response=mockMvc.perform(get(bytes(id,identity,image)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","private,no-store")).andReturn().getResponse();
            assertThat(response.getContentAsByteArray()).hasSize(image.path("bytes").asInt());
            var decoded=javax.imageio.ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));assertThat(decoded.getWidth()).isEqualTo(image.path("width").asInt());decoded.flush();
            if(image.path("role").asText().equals("READING_COVER")){assertThat(response.getContentAsByteArray()).isEqualTo(jpeg);assertThat(image.path("sourceBytes").asInt()).isEqualTo(image.path("bytes").asInt());}
            else if(image.path("role").asText().equals("READING_PAGE"))assertThat(image.path("mime").asText()).isEqualTo("image/webp");
        }
        var disk=(LocalDisk)firebaseAssetStorageClient;
        assertThat(Files.readAllBytes(disk.file(jdbcTemplate.queryForObject("select object_path from media_assets where id=?",String.class,cover)))).isEqualTo(jpeg);
        assertThat(Files.readAllBytes(disk.file(jdbcTemplate.queryForObject("select object_path from media_assets where id=?",String.class,page)))).isEqualTo(png);
        var first=manifest.path("images").get(0);String valid=bytes(id,identity,first);
        mockMvc.perform(get(valid)).andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control","private,no-store"));
        for(String invalid:List.of(valid.replace("pageNumber=0","pageNumber=1"),valid.replace("role=READING_COVER","role=READING_PAGE"),valid.replace("languageCode=tr","languageCode=en"),valid.replace("renditionKey=","renditionKey=wrong"),valid.replace("liveGeneration="+identity.path("liveGeneration").asLong(),"liveGeneration=999")))
            assertThat(mockMvc.perform(get(invalid).header("Authorization","Bearer "+token)).andReturn().getResponse().getStatus()).isBetween(400,499);
        mockMvc.perform(get(valid+"&canonicalSource=true").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        var output=jdbcTemplate.queryForObject("select manifest_json from reading_image_manifests where revision=?",String.class,identity.path("revision").asText());
        var stored=objectMapper.readTree(output).path("outputs").get(0).path("stored").path("path").asText();
        Files.write(disk.file(stored),new byte[]{9});
        mockMvc.perform(get(valid).header("Authorization","Bearer "+token)).andExpect(status().isUnprocessableEntity());
    }
    @Test void firstCorruptUploadCannotExposePreparedImagesAndRequiresExplicitRetry() throws Exception {
        long id=draft();long corrupt=upload("broken.jpg","image/jpeg",new byte[]{1,2,3});
        json("PUT","/api/admin/contents/"+id+"/localizations/tr","{\"title\":\"Bozuk kaynak\",\"description\":\"Sentetik kabul\",\"coverMediaId\":"+corrupt+",\"durationMinutes\":5,\"status\":\"DRAFT\",\"processingStatus\":\"COMPLETED\"}");
        json("POST","/api/admin/contents/"+id+"/localizations/tr/publish","{}");
        JsonNode failed=null;
        for(int i=0;i<100;i++){failed=json("GET","/api/admin/contents/"+id+"/reading-publication?languageCode=tr",null);if(failed.path("state").asText().equals("FAILED"))break;Thread.sleep(100);}
        assertThat(failed.path("state").asText()).isEqualTo("FAILED");assertThat(failed.path("liveRevision").isNull()).isTrue();assertThat(failed.path("retryable").asBoolean()).isTrue();
        mockMvc.perform(get(preview(id)).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
        json("POST","/api/admin/contents/"+id+"/reading-publication/retry?languageCode=tr",null);
        assertThat(jdbcTemplate.queryForObject("select live_proof_json from reading_publications where content_id=?",String.class,id)).isNull();
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"archive","deactivate"})
    void previewRejectsEligibilityLostDuringPinnedOutputRead(String mutation) throws Exception {
        com.tellpal.v2.asset.reading.ReadingNativeTestTools.require();long id=draft();
        long cover=upload("cover.jpg","image/jpeg",ReadingFixtures.quadrants("jpeg"));
        json("PUT","/api/admin/contents/"+id+"/localizations/tr","{\"title\":\"I/O yarisi\",\"description\":\"Sentetik kabul\",\"coverMediaId\":"+cover+",\"durationMinutes\":5,\"status\":\"DRAFT\",\"processingStatus\":\"COMPLETED\"}");
        json("POST","/api/admin/contents/"+id+"/localizations/tr/publish","{}");
        JsonNode state=null;
        for(int i=0;i<200;i++){state=json("GET","/api/admin/contents/"+id+"/reading-publication?languageCode=tr",null);if(state.path("state").asText().equals("LIVE"))break;Thread.sleep(100);}
        assertThat(state.path("state").asText()).isEqualTo("LIVE");
        var manifest=json("GET",preview(id),null);var identity=manifest.path("identity");
        var stored=objectMapper.readTree(jdbcTemplate.queryForObject("select manifest_json from reading_image_manifests where revision=?",String.class,identity.path("revision").asText())).path("outputs").get(0).path("stored").path("path").asText();
        var disk=(LocalDisk)firebaseAssetStorageClient;var changed=new java.util.concurrent.atomic.AtomicBoolean();
        disk.onPinnedRead=path->{
            if(path.equals(stored) && changed.compareAndSet(false,true)) {
                if(mutation.equals("archive")) publicationCommands.archiveLocalization(new com.tellpal.v2.content.application.ContentPublicationCommands.ArchiveContentLocalizationCommand(id,com.tellpal.v2.shared.domain.LanguageCode.from("tr")));
                else contentCommands.deleteContent(new com.tellpal.v2.content.application.ContentManagementCommands.DeleteContentCommand(id));
            }
        };
        try {
            var response=mockMvc.perform(get(bytes(id,identity,manifest.path("images").get(0))).header("Authorization","Bearer "+token))
                .andExpect(status().isConflict()).andExpect(header().string("Cache-Control","private,no-store")).andReturn().getResponse();
            assertThat(changed).isTrue();assertThat(response.getContentType()).contains("application/problem+json");
            assertThat(response.getContentAsByteArray()).startsWith((byte)'{');
            if(mutation.equals("archive"))assertThat(jdbcTemplate.queryForObject("select status from content_localizations where content_id=? and language_code='tr'",String.class,id)).isEqualTo("ARCHIVED");
            else assertThat(jdbcTemplate.queryForObject("select is_active from contents where id=?",Boolean.class,id)).isFalse();
        } finally {disk.onPinnedRead=null;}
    }

    static String preview(long id){return "/api/admin/contents/"+id+"/reading-publication/preview?languageCode=tr";}
    static String bytes(long id,JsonNode identity,JsonNode image){return "/api/admin/contents/"+id+"/reading-publication/preview/bytes?languageCode=tr&liveGeneration="+identity.path("liveGeneration").asLong()+"&revision="+identity.path("revision").asText()+"&manifestDigest="+identity.path("manifestDigest").asText()+"&jobId="+identity.path("jobId").asText()+"&role="+image.path("role").asText()+"&pageNumber="+image.path("pageNumber").asInt()+"&renditionKey="+image.path("renditionKey").asText();}
    @Test @EnabledIfEnvironmentVariable(named="TELLPAL_CMS_ACCEPTANCE",matches="true")
    void cmsUploadsBothImagesPublishesOnceAndDecodesThePreparedDialog() throws Exception {
        com.tellpal.v2.asset.reading.ReadingNativeTestTools.require();long id=draft();
        String output=System.getenv("TELLPAL_CMS_ACCEPTANCE_OUTPUT");
        var directory=output==null?Files.createTempDirectory("tellpal-cms-publish-browser-"):Files.createDirectories(Path.of(output)).resolve("browser-"+UUID.randomUUID());
        Files.createDirectories(directory);
        Files.write(directory.resolve("cover.jpg"),ReadingFixtures.quadrants("jpeg"));Files.write(directory.resolve("page.png"),ReadingFixtures.quadrants("png"));
        var session=Map.of("adminUserId",1,"username","admin-root","roleCodes",List.of("ADMIN"),"accessToken",token,"accessTokenExpiresAt",Instant.now().plusSeconds(3600).toString(),"refreshToken","synthetic-local-refresh","refreshTokenExpiresAt",Instant.now().plusSeconds(7200).toString());
        Files.writeString(directory.resolve("session.json"),objectMapper.writeValueAsString(session));
        var backendDirectory=Path.of("").toAbsolutePath();if(Files.isDirectory(backendDirectory.resolve("be")))backendDirectory=backendDirectory.resolve("be");
        var process=new ProcessBuilder("npm","exec","--","playwright","test","e2e/reading-publication-local.spec.ts","--config=playwright.local-publication.config.ts","--workers=1").directory(backendDirectory.resolve("../cms").normalize().toFile());
        process.environment().put("VITE_API_BASE_URL","http://127.0.0.1:"+port);process.environment().put("TELLPAL_CMS_ACCEPTANCE_BACKEND","http://127.0.0.1:"+port);
        process.environment().put("TELLPAL_CMS_ACCEPTANCE_CONTENT",Long.toString(id));process.environment().put("TELLPAL_CMS_ACCEPTANCE_DIRECTORY",directory.toString());
        process.redirectErrorStream(true).redirectOutput(directory.resolve("playwright.log").toFile());
        var child=process.start();boolean ended;
        try { ended=child.waitFor(120,java.util.concurrent.TimeUnit.SECONDS); }
        finally { child.descendants().forEach(ProcessHandle::destroy);if(child.isAlive())child.destroyForcibly(); }
        assertThat(ended).as("Tarayıcı kabul süresi; çıktı %s",directory).isTrue();assertThat(child.exitValue()).as("Tarayıcı kabul çıktısı: %s",directory.resolve("playwright.log")).isZero();
    }
    static final class LocalDisk extends ReadingFixtures.DiskStorage {
        volatile java.util.function.Consumer<String> onPinnedRead;
        LocalDisk(Path directory){super(directory);}
        @Override public InputStream openVersion(VersionedStorageObject object) {
            var input=super.openVersion(object);var callback=onPinnedRead;if(callback!=null)callback.accept(object.path());return input;
        }
        @Override public Path file(String path){var file=directory.resolve(path.replaceFirst("^/+","")).normalize();if(!file.startsWith(directory))throw new IllegalArgumentException("LOCAL_PATH");return file;}
        @Override public void uploadObject(String path,String mime,long size,InputStream input){try{byte[] bytes=input.readNBytes((int)size+1);if(bytes.length!=size)throw new IOException("SIZE");createImmutable(path,mime,bytes);}catch(IOException e){throw new UncheckedIOException(e);}}
        @Override public Optional<StorageObjectMetadata> findObjectMetadata(String path){return observeVersion(path).map(o->new StorageObjectMetadata(o.mime(),o.bytes()));}
        @Override public Optional<StorageObjectContent> openObject(String path,StorageObjectRange range){return observeVersion(path).map(o->{try{byte[] bytes=Files.readAllBytes(file(path));if(range!=null)bytes=Arrays.copyOfRange(bytes,(int)range.startInclusive(),(int)range.endInclusive()+1);return new StorageObjectContent(o.mime(),o.bytes(),bytes.length,range==null?null:range.startInclusive(),range==null?null:range.endInclusive(),new ByteArrayInputStream(bytes));}catch(IOException e){throw new UncheckedIOException(e);}});}
    }
}
