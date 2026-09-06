alter table asset_processing
    add column target_scope varchar(20) not null default 'LOCALIZATION';

alter table asset_processing
    alter column language_code drop not null;

alter table asset_processing
    drop constraint if exists uk_asset_processing_content_language,
    drop constraint if exists fk_asset_processing_content_localization;

alter table asset_processing
    add constraint fk_asset_processing_content
        foreign key (content_id) references contents (id) on delete cascade,
    add constraint chk_asset_processing_target_scope
        check (target_scope in ('LOCALIZATION', 'CONTENT')),
    add constraint chk_asset_processing_target_language
        check ((target_scope = 'LOCALIZATION' and language_code is not null)
            or (target_scope = 'CONTENT' and language_code is null));

create unique index uk_asset_processing_localization_target
    on asset_processing (content_id, language_code)
    where target_scope = 'LOCALIZATION';

create unique index uk_asset_processing_content_target
    on asset_processing (content_id)
    where target_scope = 'CONTENT';

create or replace function ensure_asset_processing_target_parent() returns trigger
language plpgsql
as $$
begin
    if new.target_scope = 'LOCALIZATION' then
        if not exists (
            select 1 from content_localizations
            where content_id = new.content_id and language_code = new.language_code
            for key share
        ) then
            raise exception 'Content localization not found for asset processing: content %, language %',
                new.content_id, new.language_code using errcode = '23503';
        end if;
    elsif not exists (select 1 from contents where id = new.content_id) then
        raise exception 'Content not found for asset processing: %', new.content_id using errcode = '23503';
    end if;
    return new;
end;
$$;

create trigger trg_asset_processing_target_parent
    before insert or update of target_scope, content_id, language_code on asset_processing
    for each row execute function ensure_asset_processing_target_parent();

create or replace function cascade_deleted_localization_asset_processing() returns trigger
language plpgsql
as $$
begin
    delete from asset_processing
    where target_scope = 'LOCALIZATION'
      and content_id = old.content_id
      and language_code = old.language_code;
    return old;
end;
$$;

create trigger trg_content_localizations_cascade_asset_processing
    after delete on content_localizations
    for each row execute function cascade_deleted_localization_asset_processing();

create or replace function cascade_updated_localization_asset_processing() returns trigger
language plpgsql
as $$
begin
    if old.content_id <> new.content_id or old.language_code <> new.language_code then
        update asset_processing
        set content_id = new.content_id,
            language_code = new.language_code
        where target_scope = 'LOCALIZATION'
          and content_id = old.content_id
          and language_code = old.language_code;
    end if;
    return new;
end;
$$;

create trigger trg_content_localizations_update_asset_processing
    after update of content_id, language_code on content_localizations
    for each row execute function cascade_updated_localization_asset_processing();

create index idx_asset_processing_target_lookup
    on asset_processing (target_scope, content_id, language_code);
