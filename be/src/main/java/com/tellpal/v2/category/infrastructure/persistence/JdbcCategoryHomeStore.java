package com.tellpal.v2.category.infrastructure.persistence;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.tellpal.v2.category.application.CategoryHomeStore;

@Repository
class JdbcCategoryHomeStore implements CategoryHomeStore {
    private final JdbcTemplate jdbc;
    JdbcCategoryHomeStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<Row> list(String language) {
        return jdbc.query("""
            select c.id, c.slug, l.name, l.description, c.type, c.is_active, l.status,
                   l.home_display_order, l.home_audience, l.home_content_language, l.home_version
              from categories c join category_localizations l on l.category_id=c.id
             where l.language_code=? and c.type='STORY'
             order by l.home_display_order nulls last, c.id
            """, (r,n) -> new Row(r.getLong(1), r.getString(2), r.getString(3), r.getString(4),
                r.getString(5), r.getBoolean(6), r.getString(7), (Integer) r.getObject(8),
                r.getString(9), r.getString(10), r.getLong(11)), language);
    }
    @Override public void lockLanguage(String language) {
        jdbc.queryForObject("select pg_advisory_xact_lock(hashtext(?))", Object.class, "category-home:"+language);
    }
    @Override public void deferOrderConstraint() { jdbc.execute("set constraints uk_category_home_language_order deferred"); }
    @Override public void update(String language, Row row) {
        jdbc.update("""
            update category_localizations set home_display_order=?, home_audience=?, home_content_language=?,
                home_version=home_version+1, updated_at=now() where category_id=? and language_code=?
            """, row.displayOrder(), row.audience(), row.contentLanguage(), row.categoryId(), language);
    }
    @Override public boolean localizationExists(long id, String language) {
        return jdbc.queryForObject("select count(*) from category_localizations where category_id=? and language_code=?",
                Integer.class,id,language)>0;
    }
    @Override public boolean localizationPublished(long id,String language) {
        return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from category_localizations where category_id=? and language_code=? and status='PUBLISHED')",Boolean.class,id,language));
    }
    @Override public boolean languageEnabled(String language) {
        return List.of("tr","en","pt").contains(language) && Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from languages where code=? and is_active)", Boolean.class, language));
    }
    @Override public List<Long> candidates(long id, String language, int offset, int limit) {
        return jdbc.query("select content_id from category_contents where category_id=? and language_code=? order by display_order,id limit ? offset ?",
                (r,n)->r.getLong(1),id,language,limit,offset);
    }
}
