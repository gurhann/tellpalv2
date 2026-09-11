package com.tellpal.v2.content.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.tellpal.v2.content.api.AdminContentRegistryReadiness;
import com.tellpal.v2.content.api.ContentApiType;
import com.tellpal.v2.content.application.ContentRegistryReadRepository;
import com.tellpal.v2.content.application.ContentRegistryReadRepository.RegistryCandidate;
import com.tellpal.v2.content.application.ContentRegistryReadRepository.RegistryPage;
import com.tellpal.v2.content.application.ContentRegistryReadRepository.RegistryQuery;
import com.tellpal.v2.content.application.ContentRegistryReadRepository.RegistrySnapshotRow;
import com.tellpal.v2.shared.domain.LanguageCode;

/** PostgreSQL projection for the paged, language-specific CMS content registry. */
@Repository
public class JdbcContentRegistryReadRepository implements ContentRegistryReadRepository {

    private static final String REGISTRY_CTE = """
            with registry as (
                select
                    c.id,
                    c.type,
                    c.external_key,
                    c.page_count,
                    c.is_active,
                    cl.title,
                    cl.status as localization_status,
                    cl.processing_status,
                    case
                        when c.type = 'STORY' and (
                            not c.is_active
                            or cl.id is null
                            or cl.description is null
                            or cl.cover_media_id is null
                            or c.page_count = 0
                            or coalesce(cl.processing_status, '') <> 'COMPLETED'
                            or exists (
                                select 1
                                from story_pages sp
                                left join story_page_localizations spl
                                    on spl.story_page_id = sp.id
                                   and spl.language_code = :language
                                where sp.content_id = c.id
                                  and (
                                      spl.id is null
                                      or spl.body_text is null
                                      or spl.audio_media_id is null
                                      or spl.illustration_media_id is null
                                  )
                            )
                        ) then 'ACTION_REQUIRED'
                        when c.type <> 'STORY' and (
                            not c.is_active
                            or cl.id is null
                            or coalesce(cl.processing_status, '') <> 'COMPLETED'
                        ) then 'ACTION_REQUIRED'
                        when cl.status = 'PUBLISHED' then 'PUBLISHED'
                        else 'READY_TO_PUBLISH'
                    end as readiness,
                    greatest(
                        c.updated_at,
                        coalesce(cl.updated_at, c.updated_at),
                        coalesce((
                            select max(greatest(sp.updated_at, coalesce(spl.updated_at, sp.updated_at)))
                            from story_pages sp
                            left join story_page_localizations spl
                                on spl.story_page_id = sp.id
                               and spl.language_code = :language
                            where sp.content_id = c.id
                        ), c.updated_at)
                    ) as last_edited_at
                from contents c
                left join content_localizations cl
                    on cl.content_id = c.id
                   and cl.language_code = :language
            )
            """;

    private static final String FILTERS = """
            where (:type is null or type = :type)
              and (:readiness is null or readiness = :readiness)
              and (
                  :query = ''
                  or cast(id as text) like :query escape '\\'
                  or lower(external_key) like :query escape '\\'
                  or lower(title) like :query escape '\\'
              )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcContentRegistryReadRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RegistryPage findPage(RegistryQuery query) {
        MapSqlParameterSource parameters = parameters(query);
        long totalItems = jdbcTemplate.queryForObject(
                REGISTRY_CTE + "select count(*) from registry " + FILTERS,
                parameters,
                Long.class);
        List<RegistryCandidate> candidates = jdbcTemplate.query(
                REGISTRY_CTE + """
                        select id, last_edited_at, readiness
                        from registry
                        """ + FILTERS + """
                        order by last_edited_at desc, id desc
                        limit :size offset :offset
                        """,
                parameters,
                candidateRowMapper());
        return new RegistryPage(candidates, totalItems);
    }

    @Override
    public List<RegistrySnapshotRow> findSnapshots(Collection<Long> contentIds, LanguageCode languageCode) {
        if (contentIds == null || contentIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                        select
                            c.id as content_id,
                            c.type,
                            c.external_key,
                            c.page_count,
                            c.is_active,
                            cl.title,
                            cl.description,
                            cl.cover_media_id,
                            cl.status as localization_status,
                            cl.processing_status,
                            sp.page_number as story_page_number,
                            spl.id as story_page_localization_id,
                            spl.body_text as story_page_body_text,
                            spl.audio_media_id as story_page_audio_media_id,
                            spl.illustration_media_id as story_page_illustration_media_id
                        from contents c
                        left join content_localizations cl
                            on cl.content_id = c.id
                           and cl.language_code = :language
                        left join story_pages sp
                            on sp.content_id = c.id
                        left join story_page_localizations spl
                            on spl.story_page_id = sp.id
                           and spl.language_code = :language
                        where c.id in (:contentIds)
                        order by c.id asc, sp.page_number asc
                        """,
                Map.of("contentIds", contentIds, "language", languageCode.value()),
                snapshotRowMapper());
    }

    private static MapSqlParameterSource parameters(RegistryQuery query) {
        return new MapSqlParameterSource()
                .addValue("language", query.languageCode().value())
                .addValue("type", query.type() == null ? null : query.type().name(), Types.VARCHAR)
                .addValue("readiness", query.readiness() == null ? null : query.readiness().name(), Types.VARCHAR)
                .addValue("query", toLikePattern(query.normalizedQuery()))
                .addValue("size", query.size())
                .addValue("offset", Math.multiplyExact(query.page(), query.size()));
    }

    private static String toLikePattern(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        return "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private static RowMapper<RegistryCandidate> candidateRowMapper() {
        return (resultSet, rowNumber) -> new RegistryCandidate(
                resultSet.getLong("id"),
                resultSet.getObject("last_edited_at", Timestamp.class).toInstant(),
                AdminContentRegistryReadiness.valueOf(resultSet.getString("readiness")));
    }

    private static RowMapper<RegistrySnapshotRow> snapshotRowMapper() {
        return (resultSet, rowNumber) -> new RegistrySnapshotRow(
                resultSet.getLong("content_id"),
                ContentApiType.valueOf(resultSet.getString("type")),
                resultSet.getString("external_key"),
                resultSet.getObject("page_count", Integer.class),
                resultSet.getBoolean("is_active"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getObject("cover_media_id", Long.class),
                resultSet.getString("localization_status"),
                resultSet.getString("processing_status"),
                resultSet.getObject("story_page_number", Integer.class),
                resultSet.getObject("story_page_localization_id", Long.class),
                resultSet.getString("story_page_body_text"),
                resultSet.getObject("story_page_audio_media_id", Long.class),
                resultSet.getObject("story_page_illustration_media_id", Long.class));
    }
}
