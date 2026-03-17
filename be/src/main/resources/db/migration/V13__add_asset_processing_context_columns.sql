alter table asset_processing
    add column content_type varchar(20) null,
    add column external_key varchar(180) null,
    add column cover_source_asset_id bigint null references media_assets (id),
    add column audio_source_asset_id bigint null references media_assets (id),
    add column page_count integer null;

alter table asset_processing
    add constraint chk_asset_processing_content_type check (
        content_type is null or content_type in ('STORY', 'AUDIO_STORY', 'MEDITATION', 'LULLABY')
    ),
    add constraint chk_asset_processing_external_key_not_blank check (
        external_key is null or btrim(external_key) <> ''
    ),
    add constraint chk_asset_processing_page_count_by_type check (
        content_type is null
        or
        (content_type = 'STORY' and page_count is not null and page_count >= 0)
        or
        (content_type <> 'STORY' and page_count is null)
    );

create index idx_asset_processing_context_lookup
    on asset_processing (content_type, external_key, language_code)
    where content_type is not null and external_key is not null;
