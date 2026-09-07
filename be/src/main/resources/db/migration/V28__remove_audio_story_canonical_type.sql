do $$
declare
    blocking_content record;
    blocking_category record;
    blocking_processing record;
begin
    select id, external_key
      into blocking_content
      from contents
     where type = 'AUDIO_STORY'
     order by id
     limit 1;

    if found then
        raise exception
            'V28 blocked: contents row still uses canonical AUDIO_STORY (content_id=%, external_key=%)',
            blocking_content.id,
            blocking_content.external_key;
    end if;

    select id, slug
      into blocking_category
      from categories
     where type = 'AUDIO_STORY'
     order by id
     limit 1;

    if found then
        raise exception
            'V28 blocked: categories row still uses canonical AUDIO_STORY (category_id=%, slug=%)',
            blocking_category.id,
            blocking_category.slug;
    end if;

    select id, content_id, language_code, target_scope, external_key
      into blocking_processing
      from asset_processing
     where content_type = 'AUDIO_STORY'
     order by id
     limit 1;

    if found then
        raise exception
            'V28 blocked: asset_processing row still uses canonical AUDIO_STORY '
            '(processing_id=%, content_id=%, language_code=%, target_scope=%, external_key=%)',
            blocking_processing.id,
            blocking_processing.content_id,
            blocking_processing.language_code,
            blocking_processing.target_scope,
            blocking_processing.external_key;
    end if;
end;
$$;

alter table contents
    drop constraint chk_contents_type,
    add constraint chk_contents_type check (
        type in ('STORY', 'MEDITATION', 'LULLABY')
    );

alter table categories
    drop constraint chk_categories_type,
    add constraint chk_categories_type check (
        type in ('STORY', 'MEDITATION', 'LULLABY')
    );

alter table asset_processing
    drop constraint chk_asset_processing_content_type,
    add constraint chk_asset_processing_content_type check (
        content_type is null or content_type in ('STORY', 'MEDITATION', 'LULLABY')
    );
