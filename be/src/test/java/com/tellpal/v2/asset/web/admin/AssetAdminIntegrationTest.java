package com.tellpal.v2.asset.web.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
}
