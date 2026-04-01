alter table categories
    add column type_v17 varchar(20);

do $$
declare
    blocking_category record;
begin
    select id, slug
    into blocking_category
    from categories
    where type = 'PARENT_GUIDANCE'
    order by id
    limit 1;

    if found then
        raise exception
            'V17 blocked: legacy PARENT_GUIDANCE category cannot be migrated automatically (category_id=%, slug=%)',
            blocking_category.id,
            blocking_category.slug;
    end if;

    select c.id, c.slug
    into blocking_category
    from categories c
    left join category_contents cc
        on cc.category_id = c.id
    where c.type = 'CONTENT'
    group by c.id, c.slug
    having count(cc.id) = 0
    order by c.id
    limit 1;

    if found then
        raise exception
            'V17 blocked: legacy CONTENT category has no curated content to infer a target type (category_id=%, slug=%)',
            blocking_category.id,
            blocking_category.slug;
    end if;

    select c.id, c.slug
    into blocking_category
    from categories c
    join category_contents cc
        on cc.category_id = c.id
    join contents ct
        on ct.id = cc.content_id
    where c.type = 'CONTENT'
    group by c.id, c.slug
    having count(distinct ct.type) > 1
    order by c.id
    limit 1;

    if found then
        raise exception
            'V17 blocked: legacy CONTENT category curates multiple content types (category_id=%, slug=%)',
            blocking_category.id,
            blocking_category.slug;
    end if;
end;
$$;

update categories c
set type_v17 = inferred.inferred_type
from (
    select c.id, max(ct.type) as inferred_type
    from categories c
    join category_contents cc
        on cc.category_id = c.id
    join contents ct
        on ct.id = cc.content_id
    where c.type = 'CONTENT'
    group by c.id
    having count(distinct ct.type) = 1
) inferred
where c.id = inferred.id;

do $$
declare
    unmapped_category record;
begin
    select id, slug
    into unmapped_category
    from categories
    where type_v17 is null
    order by id
    limit 1;

    if found then
        raise exception
            'V17 blocked: category type migration left an unmapped category (category_id=%, slug=%)',
            unmapped_category.id,
            unmapped_category.slug;
    end if;
end;
$$;

drop index if exists idx_categories_type_is_active;

alter table categories
    drop constraint chk_categories_type;

alter table categories
    drop column type;

alter table categories
    rename column type_v17 to type;

alter table categories
    alter column type set not null;

alter table categories
    add constraint chk_categories_type check (
        type in ('STORY', 'AUDIO_STORY', 'MEDITATION', 'LULLABY')
    );

create index idx_categories_type_is_active on categories (type, is_active);
