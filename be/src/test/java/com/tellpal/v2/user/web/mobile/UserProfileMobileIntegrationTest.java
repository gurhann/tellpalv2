package com.tellpal.v2.user.web.mobile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tellpal.v2.asset.api.AssetKind;
import com.tellpal.v2.asset.api.AssetRegistryApi;
import com.tellpal.v2.asset.api.AssetStorageProvider;
import com.tellpal.v2.asset.api.RegisterMediaAssetCommand;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest
@AutoConfigureMockMvc
class UserProfileMobileIntegrationTest extends PostgresIntegrationTestBase {

    private static final String SAMPLE_CHECKSUM =
            "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AssetRegistryApi assetRegistryApi;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                truncate table
                    user_profiles,
                    app_users,
                    media_assets
                restart identity cascade
                """);
    }

    @Test
    void firstProfileRequestCreatesDefaultProfileAndReusesItOnNextRequest() throws Exception {
        JsonNode firstPayload = readPayload(mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer stub:mobile-user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ageRange").value("UNKNOWN"))
                .andExpect(jsonPath("$[0].primary").value(true))
                .andReturn());

        long firstUserId = firstPayload.get(0).get("userId").asLong();
        long firstProfileId = firstPayload.get(0).get("profileId").asLong();

        JsonNode secondPayload = readPayload(mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer stub:mobile-user-1"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(secondPayload.get(0).get("userId").asLong()).isEqualTo(firstUserId);
        assertThat(secondPayload.get(0).get("profileId").asLong()).isEqualTo(firstProfileId);
        assertThat(jdbcTemplate.queryForObject("select count(*) from app_users", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from user_profiles", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select firebase_uid from app_users where id = ?",
                String.class,
                firstUserId)).isEqualTo("mobile-user-1");
    }

    @Test
    void updatingSecondaryProfileToPrimaryKeepsOnlyOnePrimaryProfile() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer stub:mobile-user-2"))
                .andExpect(status().isOk());

        Long userId = jdbcTemplate.queryForObject(
                "select id from app_users where firebase_uid = ?",
                Long.class,
                "mobile-user-2");
        jdbcTemplate.update(
                """
                        insert into user_profiles (
                            app_user_id,
                            display_name,
                            age_range,
                            favorite_genres,
                            main_purposes,
                            is_primary
                        ) values (?, ?, ?, ?::text[], ?::text[], ?)
                        """,
                userId,
                "Sibling",
                "5-7",
                "{adventure}",
                "{sleep}",
                false);

        Long secondProfileId = jdbcTemplate.queryForObject(
                """
                        select id
                        from user_profiles
                        where app_user_id = ? and display_name = ?
                        """,
                Long.class,
                userId,
                "Sibling");
        Long avatarMediaId = registerImageAsset("/profiles/mobile-user-2/avatar.jpg");

        mockMvc.perform(put("/api/profiles/{profileId}", secondProfileId)
                        .header("Authorization", "Bearer stub:mobile-user-2")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Sibling",
                                  "ageRange": "5-7",
                                  "avatarMediaId": %d,
                                  "favoriteGenres": ["adventure"],
                                  "mainPurposes": ["sleep"],
                                  "primary": true
                                }
                                """.formatted(avatarMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(secondProfileId))
                .andExpect(jsonPath("$.avatarMediaId").value(avatarMediaId))
                .andExpect(jsonPath("$.primary").value(true));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from user_profiles where app_user_id = ? and is_primary",
                Integer.class,
                userId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select id from user_profiles where app_user_id = ? and is_primary",
                Long.class,
                userId)).isEqualTo(secondProfileId);
    }

    @Test
    void databaseRejectsDuplicateFirebaseUid() {
        jdbcTemplate.update("insert into app_users (firebase_uid) values (?)", "duplicate-firebase-user");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into app_users (firebase_uid) values (?)",
                "duplicate-firebase-user"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void invalidStubTokenReturnsUnauthorizedProblemDetails() throws Exception {
        mockMvc.perform(get("/api/profiles")
                        .header("Authorization", "Bearer firebase-user-raw"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Firebase token"))
                .andExpect(jsonPath("$.errorCode").value("firebase_auth_error"));
    }

    private JsonNode readPayload(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private Long registerImageAsset(String objectPath) {
        return assetRegistryApi.register(new RegisterMediaAssetCommand(
                AssetStorageProvider.LOCAL_STUB,
                objectPath,
                AssetKind.ORIGINAL_IMAGE,
                "image/jpeg",
                1024L,
                SAMPLE_CHECKSUM)).assetId();
    }
}
