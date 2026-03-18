alter table media_assets
    alter column checksum_sha256 type varchar(64)
    using case
        when checksum_sha256 is null then null
        else rtrim(checksum_sha256)
    end;
