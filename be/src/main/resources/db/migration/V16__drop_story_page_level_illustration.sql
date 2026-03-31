drop index if exists idx_story_pages_illustration_media_id;

alter table story_pages
    drop column illustration_media_id;
