-- New evidence only: legacy media and editorial flags do not certify a publication.
create table reading_source_admissions (
    id uuid primary key,
    selected_asset_id bigint not null,
    object_path text not null unique,
    provider varchar(50) not null,
    asset_id bigint unique references media_assets(id),
    object_version text,
    mime_type text,
    byte_size bigint check (byte_size > 0),
    sha256 varchar(64) check (sha256 ~ '^[0-9a-f]{64}$'),
    width integer check (width > 0),
    height integer check (height > 0),
    epoch bigint not null default 1 check (epoch > 0),
    state varchar(20) not null default 'PLANNED' check (state in ('PLANNED','ADMITTED','WITHDRAWN')),
    created_at timestamptz not null default now(),
    check (state <> 'ADMITTED' or (asset_id is not null and object_version is not null and sha256 is not null and width is not null and height is not null))
);
create function reading_source_identity_guard() returns trigger language plpgsql as $$
begin
    if old.asset_id is not null and (new.id,new.selected_asset_id,new.object_path,new.provider,new.asset_id,new.object_version,new.mime_type,new.byte_size,new.sha256,new.width,new.height)
        is distinct from (old.id,old.selected_asset_id,old.object_path,old.provider,old.asset_id,old.object_version,old.mime_type,old.byte_size,old.sha256,old.width,old.height) then
        raise exception 'Admitted source identity is immutable';
    end if;
    if old.state = 'WITHDRAWN' and new.state <> 'WITHDRAWN' then raise exception 'Source withdrawal is terminal'; end if;
    return new;
end;
$$;
create trigger reading_source_identity_guard before update on reading_source_admissions for each row execute function reading_source_identity_guard();
create table reading_publication_availability (
    revision varchar(64) primary key references reading_image_jobs(revision),
    epoch bigint not null default 1 check (epoch > 0),
    valid boolean not null default false,
    checked_at timestamptz
);
create table reading_publications (
    content_id bigint not null references contents(id),
    language_code varchar(10) not null,
    generation bigint not null default 1 check (generation > 0),
    requested boolean not null default true,
    state varchar(20) not null default 'PENDING' check (state in ('PENDING','PREPARING','LIVE','FAILED','WITHDRAWN')),
    snapshot_json text not null,
    profiles_json text not null,
    effective_profile_identity varchar(64),
    live_generation bigint check (live_generation > 0),
    live_eligible boolean not null default false,
    explicit_retry boolean not null default false,
    lease uuid,
    lease_expires_at timestamptz,
    job_id uuid references reading_image_jobs(id),
    live_proof_json text,
    diagnostic varchar(80),
    updated_at timestamptz not null default now(),
    primary key(content_id,language_code),
    check ((lease is null) = (lease_expires_at is null)),
    check (state <> 'LIVE' or (live_proof_json is not null and live_generation is not null)),
    check (not live_eligible or (live_proof_json is not null and live_generation is not null))
);
create index reading_publications_pending on reading_publications(state,lease_expires_at,updated_at);
