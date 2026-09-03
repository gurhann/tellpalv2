do $$
declare
    conflicting_contributors text;
begin
    lock table contributors in share row exclusive mode;

    select string_agg(
        format('normalized_name=%L contributors=[%s]', normalized_name, contributor_details),
        '; ' order by normalized_name
    )
    into conflicting_contributors
    from (
        select
            lower(btrim(display_name)) as normalized_name,
            string_agg(
                format('id=%s,name=%L', id, display_name),
                ', ' order by id
            ) as contributor_details
        from contributors
        group by lower(btrim(display_name))
        having count(*) > 1
    ) conflicts;

    if conflicting_contributors is not null then
        raise exception
            'V20 blocked: contributor display names collide after trim and case normalization: %',
            conflicting_contributors;
    end if;
end;
$$;

alter table contributors
    add column normalized_display_name varchar(200)
        generated always as (lower(btrim(display_name))) stored;

alter table contributors
    add constraint uk_contributors_normalized_display_name
        unique (normalized_display_name);

create table contributor_roles (
    contributor_id bigint not null references contributors (id) on delete cascade,
    role varchar(20) not null,
    constraint pk_contributor_roles primary key (contributor_id, role),
    constraint chk_contributor_roles_role check (
        role in ('AUTHOR', 'ILLUSTRATOR', 'NARRATOR', 'MUSICIAN')
    )
);

create index idx_contributor_roles_role_contributor
    on contributor_roles (role, contributor_id);

insert into contributor_roles (contributor_id, role)
select distinct contributor_id, role
from content_contributors;

insert into contributor_roles (contributor_id, role)
select contributor.id, 'AUTHOR'
from contributors contributor
where not exists (
    select 1
    from contributor_roles contributor_role
    where contributor_role.contributor_id = contributor.id
);
