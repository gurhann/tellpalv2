package com.tellpal.v2.asset.web.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetMediaType;
import com.tellpal.v2.asset.api.AssetRecord;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageLocation;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.application.MediaAssetAlreadyExistsException;
import com.tellpal.v2.shared.web.admin.AdminApiExceptionHandler;
import com.tellpal.v2.shared.web.admin.AdminAuthenticationFacade;
import com.tellpal.v2.shared.web.admin.AdminProblemDetailsFactory;

@WebMvcTest(AssetAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({AssetAdminExceptionHandler.class, AdminApiExceptionHandler.class, AdminProblemDetailsFactory.class})
class AssetAdminControllerTest {

    private static final String SAMPLE_CHECKSUM =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String UPDATED_CHECKSUM =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetRegistryApi assetRegistryApi;

    @MockitoBean
    private AdminAuthenticationFacade adminAuthenticationFacade;

    @Test
    void registerAssetReturnsCreatedResponse() throws Exception {
        when(assetRegistryApi.register(any())).thenReturn(sampleAssetRecord(11L, "/assets/cover.jpg", null));

        mockMvc.perform(post("/api/admin/media")
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
                                """.formatted(SAMPLE_CHECKSUM)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/admin/media/11"))
                .andExpect(jsonPath("$.assetId").value(11))
                .andExpect(jsonPath("$.provider").value("LOCAL_STUB"))
                .andExpect(jsonPath("$.kind").value("ORIGINAL_IMAGE"));
    }

    @Test
    void listRecentAssetsReturnsArray() throws Exception {
        when(assetRegistryApi.listRecent(2)).thenReturn(List.of(
                sampleAssetRecord(11L, "/assets/cover.jpg", null),
                sampleAssetRecord(12L, "/assets/audio.mp3", null)));

        mockMvc.perform(get("/api/admin/media").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].assetId").value(11))
                .andExpect(jsonPath("$[1].assetId").value(12));
    }

    @Test
    void getMissingAssetReturnsProblemDetails() throws Exception {
        when(assetRegistryApi.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/media/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Media asset not found"))
                .andExpect(jsonPath("$.errorCode").value("media_asset_not_found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void updateMetadataReturnsUpdatedAsset() throws Exception {
        when(assetRegistryApi.updateMetadata(any())).thenReturn(sampleAssetRecord(11L, "/assets/cover.jpg", null));

        mockMvc.perform(put("/api/admin/media/11/metadata")
                        .contentType("application/json")
                        .content("""
                                {
                                  "mimeType": "image/webp",
                                  "byteSize": 2048,
                                  "checksumSha256": "%s"
                                }
                                """.formatted(UPDATED_CHECKSUM)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(11))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.byteSize").value(1024));
    }

    @Test
    void refreshDownloadUrlCacheReturnsUpdatedAsset() throws Exception {
        when(assetRegistryApi.refreshDownloadUrlCache(any())).thenReturn(sampleAssetRecord(
                11L,
                "/assets/cover.jpg",
                "http://localhost:8080/_stub/assets/cover.jpg"));

        mockMvc.perform(post("/api/admin/media/11/download-url-cache/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assetId").value(11))
                .andExpect(jsonPath("$.cachedDownloadUrl").value("http://localhost:8080/_stub/assets/cover.jpg"));
    }

    @Test
    void duplicateAssetBecomesConflictProblemDetails() throws Exception {
        when(assetRegistryApi.register(any()))
                .thenThrow(new MediaAssetAlreadyExistsException(
                        com.tellpal.v2.asset.domain.StorageProvider.LOCAL_STUB,
                        "/assets/cover.jpg"));

        mockMvc.perform(post("/api/admin/media")
                        .contentType("application/json")
                        .content("""
                                {
                                  "provider": "LOCAL_STUB",
                                  "objectPath": "/assets/cover.jpg",
                                  "kind": "ORIGINAL_IMAGE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Media asset already exists"))
                .andExpect(jsonPath("$.errorCode").value("media_asset_exists"));
    }

    private AssetRecord sampleAssetRecord(Long assetId, String objectPath, String cachedDownloadUrl) {
        Instant createdAt = Instant.parse("2026-03-17T09:00:00Z");
        return new AssetRecord(
                assetId,
                new AssetStorageLocation(AssetStorageProvider.LOCAL_STUB, objectPath),
                objectPath.endsWith(".mp3") ? AssetMediaType.AUDIO : AssetMediaType.IMAGE,
                objectPath.endsWith(".mp3") ? AssetKind.ORIGINAL_AUDIO : AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                1024L,
                SAMPLE_CHECKSUM,
                cachedDownloadUrl,
                cachedDownloadUrl == null ? null : createdAt.plusSeconds(60),
                cachedDownloadUrl == null ? null : createdAt.plusSeconds(3600),
                createdAt,
                createdAt.plusSeconds(60));
    }
}
