package com.tellpal.v2.category;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tellpal.v2.support.PostgresIntegrationTestBase;
import com.tellpal.v2.category.api.*;
import com.tellpal.v2.content.api.ContentPreview;

@SpringBootTest(properties={"tellpal.asset.processing.poller.enabled=false"})
class LibraryStoryDetailIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired LibraryStoryDetailQueryApi details;
    @BeforeEach void reset() {jdbc.execute("truncate categories,contents,contributors,media_assets restart identity cascade");}
    @Test void checksFullMembershipExactLanguageCreditsAndCmsRecommendationOrder() {
        long category=category("tr",null,"ALL");
        long first=story("first","tr",true), selected=0;
        link(category,first,"tr",0);
        for(int i=1;i<=7;i++) {long id=story("story-"+i,"tr",i!=2);link(category,id,"tr",i);if(i==7)selected=id;}
        jdbc.update("insert into content_free_access(access_key,content_id,language_code) values('default',?,'tr')",selected);
        jdbc.update("update content_localizations set description='Exact summary',duration_minutes=8 where content_id=?",selected);
        long author=jdbc.queryForObject("insert into contributors(display_name) values('Stored author') returning id",Long.class);
        long narrator=jdbc.queryForObject("insert into contributors(display_name) values('Narrator') returning id",Long.class);
        jdbc.update("insert into content_contributors(content_id,contributor_id,role,credit_name,sort_order) values(?,?,'AUTHOR','Global',0)",selected,author);
        jdbc.update("insert into content_contributors(content_id,contributor_id,role,language_code,credit_name,sort_order) values(?,?,'AUTHOR','tr','Localized',0)",selected,author);
        jdbc.update("insert into content_contributors(content_id,contributor_id,role,language_code,sort_order) values(?,?,'NARRATOR','tr',0)",selected,narrator);
        var result=query(category,selected,"tr","tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).orElseThrow();
        assertThat(result.contentId()).isEqualTo(selected); assertThat(result.summary()).isEqualTo("Exact summary");
        assertThat(result.isFree()).isTrue(); assertThat(result.isLocked()).isFalse(); assertThat(result.hero()).isNull();
        assertThat(result.credits()).extracting(c->c.displayName()).containsExactly("Localized","Narrator");
        assertThat(result.recommendations()).hasSize(5).extracting(ContentPreview::contentId).doesNotContain(selected);
        assertThat(result.recommendations().getFirst().contentId()).isEqualTo(first);
        assertThat(result.recommendations()).allSatisfy(i->{assertThat(i.contentLanguage()).isEqualTo("tr");assertThat(i.isLocked()).isTrue();});
        assertThat(query(category,selected,"tr","en",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isEmpty();
        jdbc.update("update content_localizations set status='DRAFT' where content_id=?",selected);
        assertThat(query(category,selected,"tr","tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isEmpty();
    }
    @Test void recommendationsContinuePastAFullRejectedCandidateBatchAndKeepCmsOrder() {
        long category=category("tr",null,"ALL"),selected=story("selected","tr",true);
        link(category,selected,"tr",0);
        for(int i=1;i<100;i++) link(category,story("unready-"+i,"tr",false),"tr",i);
        var eligible=new java.util.ArrayList<Long>();
        for(int i=0;i<6;i++) eligible.add(story("eligible-"+i,"tr",true));
        // Reverse insertion order so CMS order, rather than content ID, decides the result.
        for(int i=0;i<eligible.size();i++) link(category,eligible.get(i),"tr",105-i);
        var result=query(category,selected,"tr","tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).orElseThrow();
        assertThat(result.contentId()).isEqualTo(selected);
        assertThat(result.recommendations()).extracting(ContentPreview::contentId)
            .containsExactly(eligible.get(5),eligible.get(4),eligible.get(3),eligible.get(2),eligible.get(1));
        assertThat(result.recommendations()).allSatisfy(item->{
            assertThat(item.contentLanguage()).isEqualTo("tr");
            assertThat(item.isLocked()).isTrue();
        });
    }
    @Test void mappedLanguageEmptyRecommendationsAndAudienceAreServerOwned() {
        long category=category("tr","en","ALL"),id=story("mapped","en",true);link(category,id,"en",0);
        var non=query(category,id,"tr","en",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER).orElseThrow();
        assertThat(non.requestedLanguage()).isEqualTo("tr");assertThat(non.contentLanguage()).isEqualTo("en");
        assertThat(non.isLocked()).isTrue();assertThat(non.recommendations()).isEmpty();assertThat(non.credits()).isEmpty();
        assertThat(query(category,id,"tr","en",LibraryHomeQueryApi.Audience.SUBSCRIBER).orElseThrow().isLocked()).isFalse();
        jdbc.update("update category_localizations set home_audience='NON_SUBSCRIBER' where category_id=? and language_code='tr'",category);
        assertThat(query(category,id,"tr","en",LibraryHomeQueryApi.Audience.SUBSCRIBER)).isEmpty();
        jdbc.update("update category_localizations set status='DRAFT' where category_id=? and language_code='en'",category);
        assertThat(query(category,id,"tr","en",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isEmpty();
    }
    @Test void invalidIdentityAndUncuratedSelectionCannotDiscloseMetadata() {
        long category=category("tr",null,"ALL"),id=story("uncurated","tr",true);
        assertThat(query(category,id,"tr","tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isEmpty();
        assertThatThrownBy(()->details.query(category,id,"tr","tr","AUDIO_STORY",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->query(0,id,"tr","tr",LibraryHomeQueryApi.Audience.NON_SUBSCRIBER)).isInstanceOf(IllegalArgumentException.class);
    }
    private java.util.Optional<LibraryStoryDetail> query(long category,long id,String ui,String lang,LibraryHomeQueryApi.Audience audience) {
        return details.query(category,id,ui,lang,"READING",audience);
    }
    private long category(String ui,String mapping,String audience) {
        long id=jdbc.queryForObject("insert into categories(slug,type) values('detail','STORY') returning id",Long.class);
        for(String lang:List.of("tr","en","pt")) jdbc.update("insert into category_localizations(category_id,language_code,name,status,published_at,home_display_order,home_content_language,home_audience) values(?,?,?,'PUBLISHED',now(),0,?,?)",id,lang,lang+" stories",null,audience);
        jdbc.update("update category_localizations set home_content_language=? where category_id=? and language_code=?",mapping,id,ui);
        return id;
    }
    private long story(String key,String language,boolean ready) {
        long id=jdbc.queryForObject("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY',?,true,5,0) returning id",Long.class,key);
        jdbc.update("insert into content_localizations(content_id,language_code,title,status,processing_status,published_at) values(?,?,?,'PUBLISHED',?,now())",id,language,language+":"+key,ready?"COMPLETED":"PENDING");return id;
    }
    private void link(long category,long id,String lang,int order) {jdbc.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,?,?)",category,id,lang,order);}
}
