package com.tellpal.v2.support;

import java.time.Instant;
import java.util.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tellpal.v2.content.application.*;
import com.tellpal.v2.content.application.ContentManagementCommands.*;
import com.tellpal.v2.content.application.ContentFreeAccessCommands.*;
import com.tellpal.v2.content.application.ContentPublicationCommands.*;
import com.tellpal.v2.content.domain.*;
import com.tellpal.v2.shared.domain.LanguageCode;

/** Aynı uygulama komutlarıyla yalnız yeni Türkçe fixture'ı oluşturur ve yeniden bağlar. */
final class P4LocalComparisonFixture {
    static final LanguageCode TR = LanguageCode.from("tr");
    final long id;
    final Map<String,Object> localization;
    final List<Map<String,Object>> pages;
    final StoryNarrationCommand narration;
    final Instant publishedAt;

    private P4LocalComparisonFixture(long id, Map<String,Object> localization,
            List<Map<String,Object>> pages, StoryNarrationCommand narration, Instant publishedAt) {
        this.id=id; this.localization=localization; this.pages=pages; this.narration=narration; this.publishedAt=publishedAt;
    }

    static P4LocalComparisonFixture seed(ConfigurableApplicationContext context, P4LocalComparisonLedger ledger) {
        var jdbc = context.getBean(JdbcTemplate.class);
        var source = jdbc.queryForMap("select cl.*,c.age_range from content_localizations cl join contents c on c.id=cl.content_id where cl.content_id=71 and language_code='tr'");
        var pageRows = jdbc.queryForList("select sp.page_number,spl.body_text,spl.audio_media_id,spl.illustration_media_id from story_pages sp join story_page_localizations spl on spl.story_page_id=sp.id where sp.content_id=71 and spl.language_code='tr' and sp.page_number in (1,2) order by sp.page_number");
        P4LocalComparisonLedger.require(pageRows.size()==2 && number(source.get("cover_media_id"))==9778
                && number(pageRows.get(0).get("illustration_media_id"))==9816
                && number(pageRows.get(1).get("illustration_media_id"))==9818, "SOURCE_BINDINGS_CHANGED");
        var narrations = jdbc.queryForList("select audio_media_id,duration_minutes from story_narrations where content_localization_id=?", source.get("id"));
        StoryNarrationCommand narration = narrations.isEmpty() ? null : new StoryNarrationCommand(number(narrations.getFirst().get("audio_media_id")), ((Number)narrations.getFirst().get("duration_minutes")).intValue());
        var contents = context.getBean(ContentManagementService.class);
        ledger.once("CREATE_ONE_CONTENT");
        long id = contents.createContent(new CreateContentCommand(ContentType.STORY, P4LocalComparisonLedger.RUN,
                integer(source.get("age_range")), true)).contentId();
        ledger.scope(id);
        var fixture = new P4LocalComparisonFixture(id, source, pageRows, narration, Instant.now());
        contents.createLocalization(new CreateContentLocalizationCommand(id,TR,string(source,"title"),string(source,"description"),
                string(source,"body_text"),9778L,nullable(source.get("audio_media_id")),integer(source.get("duration_minutes")),
                LocalizationStatus.DRAFT,ProcessingStatus.PENDING,null,narration));
        var pageService = context.getBean(StoryPageManagementService.class);
        for (int i=0;i<2;i++) {
            var page = pageService.addStoryPage(new AddStoryPageCommand(id,null));
            P4LocalComparisonLedger.require(page.pageNumber()==i+1, "ORDERED_PAGES_REQUIRED");
            fixture.page(pageService, i, number(pageRows.get(i).get("illustration_media_id")));
        }
        context.getBean(ContentFreeAccessService.class).grantFreeAccess(new GrantContentFreeAccessCommand("default",id,TR));
        ledger.once("PUBLISH_ONCE");
        context.getBean(ContentPublicationService.class).publishLocalization(new PublishContentLocalizationCommand(id,TR,fixture.publishedAt));
        return fixture;
    }

    void rebind(ConfigurableApplicationContext context, long[] admitted) {
        P4LocalComparisonLedger.require(admitted.length==3 && Arrays.stream(admitted).distinct().count()==3, "THREE_ADMISSIONS_REQUIRED");
        context.getBean(ContentManagementService.class).updateLocalization(new UpdateContentLocalizationCommand(id,TR,
                string(localization,"title"),string(localization,"description"),string(localization,"body_text"),
                admitted[0],nullable(localization.get("audio_media_id")),integer(localization.get("duration_minutes")),
                LocalizationStatus.PUBLISHED,ProcessingStatus.PENDING,publishedAt,narration));
        var service = context.getBean(StoryPageManagementService.class);
        for (int i=0;i<2;i++) page(service,i,admitted[i+1]);
    }

    void page(StoryPageManagementService service, int index, long image) {
        var source=pages.get(index);
        service.upsertStoryPageLocalization(new UpsertStoryPageLocalizationCommand(id,index+1,TR,
                string(source,"body_text"),nullable(source.get("audio_media_id")),image));
    }
    static String string(Map<String,Object> row,String key) {return (String)row.get(key);}
    static long number(Object value) {return ((Number)value).longValue();}
    static Long nullable(Object value) {return value==null?null:number(value);}
    static Integer integer(Object value) {return value==null?null:((Number)value).intValue();}
}
