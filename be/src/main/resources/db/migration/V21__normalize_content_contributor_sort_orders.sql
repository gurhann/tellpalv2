alter table content_contributors
    drop constraint chk_content_contributors_sort_order_non_negative;

with normalized as (
    select id,
           row_number() over (
               partition by content_id, role, language_code is null, language_code
               order by sort_order, id
           ) - 1 as normalized_sort_order
    from content_contributors
)
update content_contributors assignment
set sort_order = -normalized.normalized_sort_order - 1
from normalized
where assignment.id = normalized.id;

with normalized as (
    select id,
           row_number() over (
               partition by content_id, role, language_code is null, language_code
               order by sort_order, id
           ) - 1 as normalized_sort_order
    from content_contributors
)
update content_contributors assignment
set sort_order = normalized.normalized_sort_order
from normalized
where assignment.id = normalized.id;

alter table content_contributors
    add constraint chk_content_contributors_sort_order_non_negative check (sort_order >= 0);
