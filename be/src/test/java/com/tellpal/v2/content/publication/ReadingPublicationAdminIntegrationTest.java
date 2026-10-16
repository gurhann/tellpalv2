package com.tellpal.v2.content.publication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;
import com.tellpal.v2.content.application.publication.ReadingPublicationProperties;
import com.tellpal.v2.asset.reading.ReadingFixtures;

class ReadingPublicationAdminIntegrationTest extends AdminApiIntegrationTestSupport {
    @Autowired ReadingPublicationProperties config;
    @AfterEach void disable() {config.setEnabled(false);}
    @Test void authenticatedCapabilityIsExplicitDisabledByDefaultAndLanguageValidationIsFinite() throws Exception {
        mockMvc.perform(get("/api/admin/contents/1/reading-publication").param("languageCode","tr")).andExpect(status().isUnauthorized());
        String token=authenticateAdmin();
        mockMvc.perform(get("/api/admin/contents/1/reading-publication").param("languageCode","tr").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false)).andExpect(jsonPath("$.state").value("DISABLED")).andExpect(jsonPath("$.contract").value("INTERNAL_LOCAL_TEST"));
        mockMvc.perform(post("/api/admin/contents/1/reading-publication/retry").param("languageCode","tr").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("DISABLED"));
        mockMvc.perform(get("/api/admin/contents/1/reading-publication").param("languageCode","TR").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        config.setEnabled(true);config.setProfiles(ReadingFixtures.profiles());
        long id=jdbcTemplate.queryForObject("insert into contents(type,external_key,page_count) values('STORY',?,0) returning id",Long.class,"admin-p3-"+java.util.UUID.randomUUID());
        mockMvc.perform(get("/api/admin/contents/{id}/reading-publication",id).param("languageCode","tr").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.state").value("NOT_REQUESTED"));
    }
}
