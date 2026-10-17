-- QA teslimat rezervasyonları restart ve başarısız I/O sonrasında da korunur.
create table reading_comparison_budgets (
    run_id varchar(120) primary key,
    max_operations bigint not null check (max_operations > 0),
    max_bytes bigint not null check (max_bytes > 0),
    operations bigint not null default 0 check (operations >= 0 and operations <= max_operations),
    reserved_bytes bigint not null default 0 check (reserved_bytes >= 0 and reserved_bytes <= max_bytes),
    created_at timestamptz not null default now()
);
