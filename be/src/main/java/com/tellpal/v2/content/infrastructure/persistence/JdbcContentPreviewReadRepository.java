package com.tellpal.v2.content.infrastructure.persistence;

import java.util.List;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.tellpal.v2.content.application.query.ContentPreviewReadRepository;

@Repository
class JdbcContentPreviewReadRepository implements ContentPreviewReadRepository {
    private final NamedParameterJdbcTemplate jdbc;
    JdbcContentPreviewReadRepository(NamedParameterJdbcTemplate jdbc) {this.jdbc=jdbc;}
    @Override public List<Row> findEligible(List<Long> ids,String language) {
        if(ids.isEmpty()) return List.of();
        return jdbc.query("""
            select c.id,c.external_key,l.title,c.age_range,l.duration_minutes,l.cover_media_id
            from contents c join content_localizations l on l.content_id=c.id
            where c.id in (:ids) and c.is_active and c.type='STORY' and l.language_code=:lang
              and l.status='PUBLISHED' and l.processing_status='COMPLETED'
            """,java.util.Map.of("ids",ids,"lang",language),(r,n)->new Row(r.getLong(1),r.getString(2),r.getString(3),
                r.getObject(4,Integer.class),r.getObject(5,Integer.class),r.getObject(6,Long.class)));
    }
}
