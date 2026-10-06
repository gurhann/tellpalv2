-- Existing localizations stay opted out until explicitly configured by an editor.
alter table category_localizations
    add column home_display_order integer null,
    add column home_audience varchar(20) not null default 'ALL',
    add column home_content_language varchar(8) null,
    add column home_version bigint not null default 0,
    add constraint chk_category_home_order check (home_display_order is null or home_display_order >= 0),
    add constraint chk_category_home_audience check (home_audience in ('ALL', 'NON_SUBSCRIBER')),
    add constraint uk_category_home_language_order unique (language_code, home_display_order)
        deferrable initially immediate,
    add constraint fk_category_home_curation_language foreign key (category_id, home_content_language)
        references category_localizations (category_id, language_code);
