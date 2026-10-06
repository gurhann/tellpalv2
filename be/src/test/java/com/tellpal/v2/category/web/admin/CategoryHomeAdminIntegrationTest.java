package com.tellpal.v2.category.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.category.application.CategoryHomeConfigurationService;
import com.tellpal.v2.support.AdminApiIntegrationTestSupport;

@SpringBootTest(properties={"tellpal.library.preview-limit=2", "tellpal.asset.processing.poller.enabled=false"})
@AutoConfigureMockMvc
class CategoryHomeAdminIntegrationTest extends AdminApiIntegrationTestSupport {
    @Autowired CategoryHomeConfigurationService home;
    @Autowired LibraryHomeQueryApi library;
    long row;
    @BeforeEach void reset() {
        jdbcTemplate.execute("truncate admin_refresh_tokens,admin_user_roles,admin_users,categories,contents,media_assets restart identity cascade");
        row=jdbcTemplate.queryForObject("insert into categories(slug,type) values('configured-limit','STORY') returning id",Long.class);
        jdbcTemplate.update("insert into category_localizations(category_id,language_code,name,status,published_at) values(?,'tr','Configured limit','PUBLISHED',now())",row);
    }
    @Test void invalidHomeWritesUseTheSharedProblemDetailHandlerAndPreserveSecurityAndConflicts() throws Exception {
        String token=authenticateAdmin();
        String path="/api/admin/categories/"+row+"/localizations/tr/home";
        for(String body:List.of(
                "{\"displayOrder\":-1,\"audience\":\"ALL\"}",
                "{\"displayOrder\":0,\"audience\":\"INVALID\"}",
                "{\"displayOrder\":0,\"audience\":\"ALL\",\"contentLanguage\":\"en\"}")) {
            mockMvc.perform(put(path).header("Authorization","Bearer "+token).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.errorCode").value("invalid_request"));
        }
        mockMvc.perform(put(path).header("Authorization","Bearer "+token).contentType("application/json").content("{bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("invalid_body"));
        mockMvc.perform(get("/api/admin/categories/home?lang=unknown").header("Authorization","Bearer "+token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("invalid_request"));
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/categories/999/localizations/tr/home").header("Authorization","Bearer "+token))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/admin/categories/home?lang=tr").header("Authorization","Bearer "+token)
                .contentType("application/json").content("{\"expectedRevision\":\"obsolete\",\"rows\":[]}"))
                .andExpect(status().isConflict());
        assertThat(home.read(row,"tr").displayOrder()).isNull();
    }
    @Test void nonDefaultConfiguredLimitIsAppliedAfterInactiveAndDraftCandidatesInPostgres() {
        List<Long> eligible=new ArrayList<>();
        for(int index=0;index<7;index++) {
            long id=jdbcTemplate.queryForObject("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY',?,?,5,0) returning id",Long.class,"limit-"+index,index!=0);
            jdbcTemplate.update("insert into content_localizations(content_id,language_code,title,status,processing_status,published_at) values(?,'tr',?,?,'COMPLETED',now())",id,"Title "+index,index==1?"DRAFT":"PUBLISHED");
            jdbcTemplate.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,'tr',?)",row,id,index);
            if(index>1) eligible.add(id);
        }
        home.save(row,"tr",new CategoryHomeConfigurationService.Configuration(row,0,"ALL",null));
        var catalog=library.query("tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER);
        assertThat(catalog.previewLimit()).isEqualTo(2);
        assertThat(catalog.rows()).hasSize(1);
        assertThat(catalog.rows().getFirst().items()).extracting(com.tellpal.v2.content.api.ContentPreview::contentId)
                .containsExactlyElementsOf(eligible.subList(0,2));
    }
}
