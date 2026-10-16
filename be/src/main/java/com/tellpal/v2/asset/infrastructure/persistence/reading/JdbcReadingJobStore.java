package com.tellpal.v2.asset.infrastructure.persistence.reading;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.Clock;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;

/** PostgreSQL uniqueness and row locks fence the complete captured attempt and atomic manifest commit. */
@Repository
public class JdbcReadingJobStore implements ReadingJobStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ObjectMapper mapper = new ObjectMapper().enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    public JdbcReadingJobStore(JdbcTemplate jdbc, PlatformTransactionManager manager, Clock clock) {
        this.jdbc=jdbc; this.clock=clock; this.transaction=new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public Preparation schedule(ReadingWork work) { return schedule(work,null); }
    @Override public Preparation schedule(ReadingWork work,ReadingDeadline deadline) {
        return transaction.execute(s -> {
            checkDeadline(deadline);
            guardCommit(deadline,null,0);
            UUID id=UUID.randomUUID();
            jdbc.update("insert into reading_image_jobs(id,revision,work_json,source_set,profile_set) values (?,?,?,?,?) on conflict(revision) do nothing",
                    id,work.revision(),json(work),work.sourceSet(),work.profileSet());
            var row=jdbc.queryForMap("select * from reading_image_jobs where revision=? for update",work.revision());
            if(!work((UUID)row.get("id")).sameCapturedIdentity(work)) throw new ReadingAttemptRejectedException("REVISION_CAPTURE_CONFLICT");
            var scheduled=result(row);
            checkDeadline(deadline);
            return scheduled;
        });
    }
    @Override public Preparation find(UUID id) { return result(row(id,false)); }
    @Override public ReadingWork work(UUID id) {
        try { return mapper.readValue((String)row(id,false).get("work_json"),ReadingWork.class); }
        catch(java.io.IOException e) { throw new IllegalStateException("Captured work cannot be decoded",e); }
    }
    @Override public Optional<UUID> next(long now) {
        var ids=jdbc.queryForList("select id from reading_image_jobs where status='PENDING' or (status='PROCESSING' and lease_expires_at<=?) order by created_at limit 1",
                UUID.class,time(now));
        return ids.stream().findFirst();
    }
    @Override public Optional<Attempt> claim(UUID id,long now) {
        return transaction.execute(s -> {
            var row=row(id,true); long ownedNow=Math.max(now,clock.millis()); String state=(String)row.get("status");
            if(!state.equals("PENDING") && !(state.equals("PROCESSING") && !instant(row).isAfter(Instant.ofEpochMilli(ownedNow)))) return Optional.empty();
            var work=work(id);
            long duration=work.profiles().values().stream().mapToLong(p -> p.limits().revisionMillis()).max().orElseThrow();
            int number=((Number)row.get("attempt")).intValue()+1; UUID lease=UUID.randomUUID();
            long expiry=Math.addExact(ownedNow,Math.addExact(duration,5000));
            jdbc.update("update reading_image_jobs set status='PROCESSING',attempt=?,lease=?,lease_expires_at=?,diagnostic=null,updated_at=now() where id=?",
                    number,lease,time(expiry),id);
            return Optional.of(new Attempt(id,number,lease,expiry,work.sourceSet(),work.profileSet()));
        });
    }
    @Override public Preparation retry(UUID id) {
        return transaction.execute(s -> {
            var row=row(id,true);
            if(!row.get("status").equals("FAILED")) throw new ReadingAttemptRejectedException("EXPLICIT_RETRY_REQUIRES_FAILED");
            jdbc.update("update reading_image_jobs set status='PENDING',diagnostic=null,updated_at=now() where id=?",id);
            return find(id);
        });
    }
    @Override public Preparation succeed(Attempt token,Manifest manifest,long now) { return succeed(token,manifest,now,null); }
    @Override public Preparation succeed(Attempt token,Manifest manifest,long now,ReadingDeadline deadline) {
        return transaction.execute(s -> {
            var row=row(token.jobId(),true);
            checkDeadline(deadline);
            requireToken(row,token);
            if(row.get("status").equals("COMPLETED")) {
                var saved=result(row);
                if(!manifest.digest().equals(saved.manifestDigest()) || !manifest.canonical().equals(saved.manifest())) throw new ReadingAttemptRejectedException("CONTRADICTORY_SUCCESS");
                return saved;
            }
            requireProcessing(row,token,Math.max(now,clock.millis()));
            guardCommit(deadline,token,now);
            work(token.jobId()).requireCompleteManifest(manifest);
            jdbc.update("insert into reading_image_manifests(revision,job_id,digest,manifest_json) values(?,?,?,?) on conflict(revision) do nothing",
                    manifest.revision(),token.jobId(),manifest.digest(),manifest.canonical());
            var existing=jdbc.queryForMap("select * from reading_image_manifests where revision=?",manifest.revision());
            if(!existing.get("manifest_json").equals(manifest.canonical()) || !existing.get("digest").equals(manifest.digest())
                    || !existing.get("job_id").equals(token.jobId())) throw new ReadingAttemptRejectedException("MANIFEST_CONFLICT");
            checkDeadline(deadline);
            requireProcessing(row,token,Math.max(now,clock.millis()));
            jdbc.update("update reading_image_jobs set status='COMPLETED',manifest_digest=?,updated_at=now() where id=?",manifest.digest(),token.jobId());
            var completed=find(token.jobId());
            checkDeadline(deadline);
            return completed;
        });
    }
    @Override public Preparation fail(Attempt token,String diagnostic,long now) {
        return transaction.execute(s -> {
            var row=row(token.jobId(),true); requireToken(row,token);
            if(row.get("status").equals("FAILED") && Objects.equals(row.get("diagnostic"),diagnostic)) return result(row);
            requireProcessing(row,token,Math.max(now,clock.millis()));
            guardCommit(null,token,now);
            jdbc.update("update reading_image_jobs set status='FAILED',diagnostic=?,updated_at=now() where id=?",diagnostic,token.jobId());
            return find(token.jobId());
        });
    }
    @Override public void evidence(Attempt t,String kind,String detail) {
        jdbc.update("insert into reading_image_attempt_evidence(job_id,attempt,lease,kind,detail) values(?,?,?,?,?) on conflict do nothing",
                t.jobId(),t.number(),t.lease(),kind,detail);
    }
    @Override public void integrity(String revision,boolean valid,String detail) {
        transaction.executeWithoutResult(s -> {
            jdbc.update("insert into reading_image_integrity(revision,valid,detail) values(?,?,?)",revision,valid,detail);
            if(!valid) jdbc.update("insert into reading_publication_availability(revision,valid) values(?,false) on conflict(revision) do update set valid=false,epoch=reading_publication_availability.epoch+1",revision);
        });
    }
    @Override public void planCapture(UUID owner,String path,String provider,String sha256) {
        jdbc.update("insert into reading_image_source_captures(capture_id,object_path,provider,sha256) values(?,?,?,?) on conflict do nothing",owner,path,provider,sha256);
    }
    @Override public void observeCapture(UUID owner,String path,String version) {
        jdbc.update("update reading_image_source_captures set object_version=? where capture_id=? and object_path=?",version,owner,path);
    }
    @Override public void attachCapture(UUID owner,UUID jobId) {
        jdbc.update("update reading_image_source_captures set job_id=? where capture_id=?",jobId,owner);
    }
    private Map<String,Object> row(UUID id,boolean lock) {
        return jdbc.queryForMap("select * from reading_image_jobs where id=?"+(lock?" for update":""),id);
    }
    private Preparation result(Map<String,Object> row) {
        String manifest=null;
        if(row.get("manifest_digest")!=null) manifest=jdbc.queryForObject("select manifest_json from reading_image_manifests where revision=?",String.class,row.get("revision"));
        return new Preparation((UUID)row.get("id"),(String)row.get("revision"),State.valueOf((String)row.get("status")),
                (String)row.get("manifest_digest"),manifest,(String)row.get("diagnostic"));
    }
    private static void requireToken(Map<String,Object> row,Attempt t) {
        if(!Objects.equals(row.get("id"),t.jobId()) || ((Number)row.get("attempt")).intValue()!=t.number()
                || !Objects.equals(row.get("lease"),t.lease()) || instant(row).toEpochMilli()!=t.expiresEpochMillis()
                || !Objects.equals(row.get("source_set"),t.sourceSet()) || !Objects.equals(row.get("profile_set"),t.profileSet()))
            throw new ReadingAttemptRejectedException("STALE_READING_ATTEMPT");
    }
    private static void requireProcessing(Map<String,Object> row,Attempt t,long now) {
        if(!row.get("status").equals("PROCESSING") || now>=t.expiresEpochMillis()) throw new ReadingAttemptRejectedException("NON_OWNING_TERMINAL_CALLBACK");
    }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalArgumentException(e); } }
    private void guardCommit(ReadingDeadline deadline,Attempt token,long callerNow) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) {
                        checkDeadline(deadline);
                        if(token!=null && Math.max(callerNow,clock.millis())>=token.expiresEpochMillis())
                            throw new ReadingAttemptRejectedException("NON_OWNING_TERMINAL_CALLBACK");
                    }
                });
    }
    private static void checkDeadline(ReadingDeadline deadline) { if(deadline!=null) deadline.check(); }
    private static Timestamp time(long now) { return Timestamp.from(Instant.ofEpochMilli(now)); }
    private static Instant instant(Map<String,Object> row) { return ((Timestamp)row.get("lease_expires_at")).toInstant(); }
}
