package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.tellpal.v2.content.application.query.StoryDetailMetadataReadRepository;
import com.tellpal.v2.content.api.StoryDetailMetadata.Credit;

@Repository
class JdbcStoryDetailMetadataReadRepository implements StoryDetailMetadataReadRepository {
    private final JdbcTemplate jdbc;
    JdbcStoryDetailMetadataReadRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<Row> findEligible(long id, String language) {
        return jdbc.query("""
            select c.id,c.external_key,l.title,c.age_range,l.duration_minutes,l.description,l.cover_media_id
            from contents c join content_localizations l on l.content_id=c.id
            where c.id=? and c.is_active and c.type='STORY' and l.language_code=?
              and l.status='PUBLISHED' and l.processing_status='COMPLETED'
            """, (r,n) -> new Row(r.getLong(1),r.getString(2),r.getString(3),r.getObject(4,Integer.class),
                r.getObject(5,Integer.class),r.getString(6),r.getObject(7,Long.class)),id,language).stream().findFirst();
    }
    @Override public List<Credit> credits(long id, String language) {
        return jdbc.query("""
            select contributor_id,role,display_name from (
              select distinct on (a.contributor_id,a.role) a.contributor_id,a.role,
                coalesce(a.credit_name,c.display_name) display_name,a.sort_order,a.id
              from content_contributors a join contributors c on c.id=a.contributor_id
              where a.content_id=? and (a.language_code=? or a.language_code is null)
              order by a.contributor_id,a.role,a.language_code nulls last,a.id
            ) resolved
            order by case role when 'AUTHOR' then 0 when 'NARRATOR' then 1
                when 'ILLUSTRATOR' then 2 else 3 end,sort_order,id
            limit 101
            """, (r,n) -> new Credit(r.getLong(1),r.getString(2),r.getString(3)),id,language);
    }
}
