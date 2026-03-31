alter table story_page_localizations
    add column illustration_media_id bigint null;

update story_page_localizations localization
set illustration_media_id = story_page.illustration_media_id
from story_pages story_page
where localization.story_page_id = story_page.id
  and localization.illustration_media_id is null;

do $$
declare
    missing_illustration_count integer;
begin
    select count(*)
    into missing_illustration_count
    from story_page_localizations
    where illustration_media_id is null;

    if missing_illustration_count > 0 then
        raise exception
            'Cannot localize story page illustrations because % story_page_localizations rows still lack illustration_media_id after backfill.',
            missing_illustration_count
            using errcode = '23502';
    end if;
end;
$$;

alter table story_page_localizations
    add constraint chk_story_page_localizations_illustration_media_id_positive check (
        illustration_media_id > 0
    );

create index idx_story_page_localizations_illustration_media_id
    on story_page_localizations (illustration_media_id);

alter table story_page_localizations
    alter column illustration_media_id set not null;
