package com.tellpal.v2.asset.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.asset.infrastructure.storage.FakeFirebaseStorageAssetClient;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class AssetAdminIntegrationTest extends AdminApiIntegrationTestSupport {

    private static final String INITIAL_CHECKSUM =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String UPDATED_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @org.springframework.beans.factory.annotation.Autowired
    private FakeFirebaseStorageAssetClient fakeFirebaseStorageAssetClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    admin_refresh_tokens,
                    admin_user_roles,
                    admin_users,
                    media_assets
                restart identity cascade
                """);
        fakeFirebaseStorageAssetClient.clearUploadedObjects();
    }

    @Test
    void assetCreateListUpdateAndRefreshWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/admin/media")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider": "LOCAL_STUB",
                                  "objectPath": "/assets/cover.jpg",
                                  "kind": "ORIGINAL_IMAGE",
                                  "mimeType": "image/jpeg",
                                  "byteSize": 1024,
                                  "checksumSha256": "%s"
                                }
                                """.formatted(INITIAL_CHECKSUM)))
                .andExpect(status().isCreated())
                .andReturn();

        Long assetId = readPayload(createResult).get("assetId").asLong();

        mockMvc.perform(get("/api/admin/media")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assetId").value(assetId));

        mockMvc.perform(put("/api/admin/media/{assetId}/metadata", assetId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "mimeType": "image/webp",
                                  "byteSize": 2048,
                                  "checksumSha256": "%s"
                                }
                                """.formatted(UPDATED_CHECKSUM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byteSize").value(2048));

        mockMvc.perform(post("/api/admin/media/{assetId}/download-url-cache/refresh", assetId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cachedDownloadUrl").isNotEmpty());
    }

    @Test
    void duplicateAssetRegistrationReturnsConflict() throws Exception {
        String accessToken = authenticateAdmin();

        mockMvc.perform(post("/api/admin/media")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider": "LOCAL_STUB",
                                  "objectPath": "/assets/duplicate.jpg",
                                  "kind": "ORIGINAL_IMAGE"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/media")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider": "LOCAL_STUB",
                                  "objectPath": "/assets/duplicate.jpg",
                                  "kind": "ORIGINAL_IMAGE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("media_asset_exists"));
    }

    @Test
    void initiateCompleteAndRefreshFirebaseUploadWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();

        MvcResult initiateResult = mockMvc.perform(post("/api/admin/media/uploads")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "kind": "ORIGINAL_AUDIO",
                                  "fileName": "bedtime-breeze.mp3",
                                  "mimeType": "audio/mpeg",
                                  "byteSize": 8192
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("FIREBASE_STORAGE"))
                .andReturn();

        String objectPath = readPayload(initiateResult).get("objectPath").asText();
        String uploadToken = readPayload(initiateResult).get("uploadToken").asText();
        fakeFirebaseStorageAssetClient.storeUploadedObject(objectPath, "audio/mpeg", 8192L);

        MvcResult completeResult = mockMvc.perform(post("/api/admin/media/uploads/complete")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "uploadToken": "%s",
                                  "checksumSha256": "%s"
                                }
                                """.formatted(uploadToken, INITIAL_CHECKSUM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("FIREBASE_STORAGE"))
                .andExpect(jsonPath("$.objectPath").value(objectPath))
                .andReturn();

        Long assetId = readPayload(completeResult).get("assetId").asLong();

        mockMvc.perform(post("/api/admin/media/{assetId}/download-url-cache/refresh", assetId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cachedDownloadUrl").value(org.hamcrest.Matchers.containsString("firebase-storage.test/download/")));
    }

    @Test
    void proxiedFirebaseUploadWorksWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();

        byte[] imageBytes = "image-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MvcResult initiateResult = mockMvc.perform(post("/api/admin/media/uploads")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/json")
                        .content("""
                                {
                                  "kind": "ORIGINAL_IMAGE",
                                  "fileName": "cover.jpg",
                                  "mimeType": "image/jpeg",
                                  "byteSize": %d
                                }
                                """.formatted(imageBytes.length)))
                .andExpect(status().isOk())
                .andReturn();

        String objectPath = readPayload(initiateResult).get("objectPath").asText();
        String uploadToken = readPayload(initiateResult).get("uploadToken").asText();
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", imageBytes);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/admin/media/uploads/proxy")
                        .file(file)
                        .param("uploadToken", uploadToken)
                        .param("checksumSha256", INITIAL_CHECKSUM)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("FIREBASE_STORAGE"))
                .andExpect(jsonPath("$.objectPath").value(objectPath))
                .andExpect(jsonPath("$.byteSize").value(imageBytes.length));
    }

    @Test
    void backendUploadAndContentStreamingWorkWithAuthenticatedAdmin() throws Exception {
        String accessToken = authenticateAdmin();
        byte[] imageBytes = "image-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", "image/jpeg", imageBytes);

        MvcResult uploadResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/admin/media/uploads")
                        .file(file)
                        .param("kind", "ORIGINAL_IMAGE")
                        .param("checksumSha256", INITIAL_CHECKSUM)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("FIREBASE_STORAGE"))
                .andExpect(jsonPath("$.byteSize").value(imageBytes.length))
                .andReturn();

        Long assetId = readPayload(uploadResult).get("assetId").asLong();

        MvcResult tokenResult = mockMvc.perform(post("/api/admin/media/{assetId}/content-token", assetId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewUrl").isNotEmpty())
                .andReturn();

        String previewUrl = readPayload(tokenResult).get("previewUrl").asText();
        String token = new URI(previewUrl).getQuery().replace("token=", "");

        mockMvc.perform(get("/api/admin/media/{assetId}/content", assetId)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(content().bytes(imageBytes));

        mockMvc.perform(get("/api/admin/media/{assetId}/content", assetId)
                        .param("token", token)
                        .header("Range", "bytes=0-4"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-4/%d".formatted(imageBytes.length)))
                .andExpect(content().bytes("image".getBytes(StandardCharsets.UTF_8)));
    }
}
