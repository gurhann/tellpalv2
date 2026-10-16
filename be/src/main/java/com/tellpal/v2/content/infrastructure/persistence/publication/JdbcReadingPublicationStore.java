package com.tellpal.v2.content.infrastructure.persistence.publication;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Snapshot;
import com.tellpal.v2.asset.api.ReadingPublicationReadyApi.Proof;
import com.tellpal.v2.content.domain.publication.ReadingPublication;

/** Content-owned SQL only. Callers hold the aggregate row before the scope row in the same transaction. */
@Repository
public class JdbcReadingPublicationStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    public JdbcReadingPublicationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> pending(java.util.Map<?, ?> profiles, String effectiveIdentity) {
        return jdbc.queryForList("""
                select content_id,language_code from reading_publications
                where requested
                and (state in ('PENDING','PREPARING') or profiles_json<>? or effective_profile_identity is distinct from ?)
                and (lease is null or lease_expires_at<=now())
                order by updated_at limit 20
                """, json(profiles), effectiveIdentity);
    }

    public Optional<Map<String, Object>> row(long id, String language) {
        return jdbc.queryForList("""
                select * from reading_publications
                where content_id=?
                and language_code=? for update
                """, id, language).stream().findFirst();
    }

    public void request(Snapshot snapshot, java.util.Map<?, ?> profiles) {
        jdbc.update("""
                insert into reading_publications(content_id,language_code,snapshot_json,profiles_json)
                values(?,?,?,?)
                on conflict(content_id,language_code) do update
                set requested=true,explicit_retry=(reading_publications.state='FAILED'),state='PENDING',generation=reading_publications.generation+1,snapshot_json=excluded.snapshot_json,profiles_json=excluded.profiles_json,effective_profile_identity=null,job_id=null,lease=null,lease_expires_at=null,diagnostic=null,updated_at=now()
                where not reading_publications.requested or reading_publications.state='FAILED'
                """, snapshot.contentId(), snapshot.language(), json(snapshot), json(profiles));
    }

    public void changed(Snapshot snapshot, boolean eligible, java.util.Map<?, ?> profiles) {
        jdbc.update("""
                update reading_publications
                set generation=generation+1,explicit_retry=false,snapshot_json=?,profiles_json=?,effective_profile_identity=null,live_eligible=live_eligible
                and ?,state=case when requested
                and ? then 'PENDING' else 'WITHDRAWN' end,requested=requested
                and ?,job_id=null,lease=null,lease_expires_at=null,diagnostic=null,updated_at=now()
                where content_id=?
                and language_code=?
                """, json(snapshot), json(profiles), eligible, eligible, eligible, snapshot.contentId(), snapshot.language());
    }

    public void refreshProfiles(long id, String language, java.util.Map<?, ?> profiles, String effectiveIdentity) {
        jdbc.update("""
                update reading_publications set effective_profile_identity=?
                where content_id=? and language_code=? and requested
                and effective_profile_identity is null and profiles_json=?
                """, effectiveIdentity, id, language, json(profiles));
        jdbc.update("""
                update reading_publications
                set generation=generation+1,profiles_json=?,effective_profile_identity=?,state='PENDING',job_id=null,
                    lease=null,lease_expires_at=null,explicit_retry=false,updated_at=now()
                where content_id=? and language_code=? and requested
                and (profiles_json<>? or effective_profile_identity is distinct from ?)
                """, json(profiles), effectiveIdentity, id, language, json(profiles), effectiveIdentity);
    }

    public Optional<ReadingPublication> claim(long id, String language) {
        var row = row(id, language).orElseThrow();
        if (!Boolean.TRUE.equals(row.get("requested")) || !Set.of("PENDING", "PREPARING").contains(row.get("state"))) return Optional.empty();
        if (row.get("lease_expires_at") != null && ((java.sql.Timestamp)row.get("lease_expires_at")).toInstant().isAfter(java.time.Instant.now())) return Optional.empty();
        UUID lease = UUID.randomUUID();
        var expires = jdbc.queryForObject("""
                update reading_publications
                set lease=?,lease_expires_at=now()+interval '5 minutes',state='PREPARING'
                where content_id=?
                and language_code=?
                returning lease_expires_at
                """, java.sql.Timestamp.class, lease, id, language);
        return Optional.of(new ReadingPublication(id, language, ((Number)row.get("generation")).longValue(), lease, expires.getTime(), decode((String)row.get("snapshot_json"), Snapshot.class), (UUID)row.get("job_id"), Boolean.TRUE.equals(row.get("explicit_retry")), (String)row.get("effective_profile_identity")));
    }

    public boolean owns(ReadingPublication claim) {
        var row = row(claim.contentId(), claim.language()).orElseThrow();
        return Boolean.TRUE.equals(row.get("requested")) && row.get("state").equals("PREPARING")
                && ((Number) row.get("generation")).longValue() == claim.generation()
                && Objects.equals(claim.effectiveProfileIdentity(), row.get("effective_profile_identity"))
                && claim.lease().equals(row.get("lease"))
                && ((java.sql.Timestamp) row.get("lease_expires_at")).getTime() == claim.leaseExpiresEpochMillis()
                && ((java.sql.Timestamp) row.get("lease_expires_at")).toInstant().isAfter(java.time.Instant.now());
    }

    public void attach(ReadingPublication claim, UUID job) {
        if (owns(claim)) {
            fenceCommit(claim);
            jdbc.update("""
                update reading_publications
                set job_id=?,explicit_retry=false,lease=null,lease_expires_at=null,updated_at=now()
                where content_id=?
                and language_code=?
                """, job, claim.contentId(), claim.language());
        }
    }

    public void fail(ReadingPublication claim, String code) {
        if (owns(claim)) {
            fenceCommit(claim);
            jdbc.update("""
                update reading_publications
                set state='FAILED',diagnostic=?,lease=null,lease_expires_at=null,updated_at=now()
                where content_id=?
                and language_code=?
                """, code, claim.contentId(), claim.language());
        }
    }

    public void adopt(ReadingPublication claim, Proof proof) {
        if (owns(claim)) {
            fenceCommit(claim);
            jdbc.update("""
                update reading_publications
                set state='LIVE',live_eligible=true,live_generation=?,live_proof_json=?,job_id=?,lease=null,lease_expires_at=null,diagnostic=null,updated_at=now()
                where content_id=?
                and language_code=?
                """, claim.generation(), json(proof), proof.jobId(), claim.contentId(), claim.language());
        }
    }

    private static void fenceCommit(ReadingPublication claim) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
    public void beforeCommit(boolean readOnly) {
                if (System.currentTimeMillis() >= claim.leaseExpiresEpochMillis()) throw new IllegalStateException("PUBLICATION_LEASE_EXPIRED");
            }
        });
    }

    public void unavailable(long id, String language) {
        jdbc.update("""
                update reading_publications
                set state='FAILED',diagnostic='RETAINED_MEDIA_UNAVAILABLE'
                where content_id=?
                and language_code=?
                and state='LIVE'
                """, id, language);
    }

    public void retry(long id, String language) {
        jdbc.update("""
                update reading_publications
                set state='PENDING',job_id=null,explicit_retry=true,lease=null,lease_expires_at=null,diagnostic=null,updated_at=now()
                where content_id=?
                and language_code=?
                and requested
                and state='FAILED'
                """, id, language);
    }

    public <T> T decode(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("INVALID_PUBLICATION_EVIDENCE", e);
        }
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
