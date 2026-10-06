package com.tellpal.v2.category.migration;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import com.tellpal.v2.category.api.LibraryHomeQueryApi;
import com.tellpal.v2.support.PostgresIntegrationTestBase;

@SpringBootTest(properties="tellpal.asset.processing.poller.enabled=false")
class LibraryHomeOptInMigrationIntegrationTest extends PostgresIntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired LibraryHomeQueryApi library;

    @Test void eligiblePublishedV32CurationRemainsIntactAndOptedOutAfterUpgradingLatest() {
        var oldSchema = Flyway.configure().dataSource(jdbc.getDataSource()).locations("classpath:db/migration")
                .cleanDisabled(false).target(MigrationVersion.fromVersion("32")).load();
        oldSchema.clean();
        oldSchema.migrate();
        assertThat(oldSchema.info().current().getVersion().getVersion()).isEqualTo("32");
        long row=jdbc.queryForObject("insert into categories(slug,type,is_active) values('pre-v33-library','STORY',true) returning id",Long.class);
        for(String language:List.of("tr","en")) {
            jdbc.update("insert into category_localizations(category_id,language_code,name,status,published_at) values(?,?,?,'PUBLISHED',now())",row,language,"Existing "+language);
        }
        long first=story("existing-first");long second=story("existing-second");
        for(String language:List.of("tr","en")) {
            for(long content:List.of(first,second)) jdbc.update("insert into content_localizations(content_id,language_code,title,status,processing_status,published_at) values(?,?,?,'PUBLISHED','COMPLETED',now())",content,language,"Existing "+content);
            jdbc.update("insert into category_contents(category_id,content_id,language_code,display_order) values(?,?,?,0),(?,?,?,1)",row,second,language,row,first,language);
        }
        assertThat(jdbc.queryForObject("select count(*) from category_contents cc join categories c on c.id=cc.category_id join category_localizations cl on cl.category_id=c.id and cl.language_code=cc.language_code join contents x on x.id=cc.content_id join content_localizations xl on xl.content_id=x.id and xl.language_code=cc.language_code where c.is_active and x.is_active and cl.status='PUBLISHED' and xl.status='PUBLISHED' and xl.processing_status='COMPLETED'",Integer.class)).isEqualTo(4);
        var originalCuration=jdbc.queryForList("select * from category_contents order by language_code,display_order");
        var latest=Flyway.configure().dataSource(jdbc.getDataSource()).locations("classpath:db/migration").load();
        latest.migrate();
        assertThat(latest.info().current().getVersion().getVersion()).isNotEqualTo("32");
        assertThat(jdbc.queryForList("select * from category_contents order by language_code,display_order")).isEqualTo(originalCuration);
        var localizations=jdbc.queryForList("select home_display_order,home_audience,home_content_language,home_version from category_localizations where category_id=?",row);
        assertThat(localizations).hasSize(2).allSatisfy(localization->{
            assertThat(localization.get("home_display_order")).isNull();assertThat(localization.get("home_content_language")).isNull();
            assertThat(localization.get("home_audience")).isEqualTo("ALL");assertThat(localization.get("home_version")).isEqualTo(0L);
        });
        for(String language:List.of("tr","en")) for(var audience:LibraryHomeQueryApi.Audience.values()) {
            assertThat(library.query(language,audience).rows()).isEmpty();
        }
    }
    private long story(String key) {
        return jdbc.queryForObject("insert into contents(type,external_key,is_active,age_range,page_count) values('STORY',?,true,5,0) returning id",Long.class,key);
    }
}
