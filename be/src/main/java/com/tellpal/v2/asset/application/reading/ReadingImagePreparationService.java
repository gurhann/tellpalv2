package com.tellpal.v2.asset.application.reading;

import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi;
import com.tellpal.v2.asset.domain.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;
import com.tellpal.v2.asset.infrastructure.storage.*;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingPreservation;

/** Captures immutable byte proof only; P3 must independently compare content bindings/current generation. */
@Service
public class ReadingImagePreparationService implements ReadingImagePreparationApi {
    private final MediaAssetRepository assets;
    private final ReadingObjectStorage storage;
    private final ReadingJobStore jobs;
    private final Environment environment;
    private final String prefix;
    private final boolean enabled;
    private final ReadingSourceAdmissionService admissions;
    private final ObjectMapper mapper=new ObjectMapper();
    public ReadingImagePreparationService(MediaAssetRepository assets,ReadingObjectStorage storage,ReadingJobStore jobs,
            Environment environment,AssetStorageFirebaseProperties properties,@Value("${tellpal.asset.reading.test-engine-enabled:false}") boolean enabled, ReadingSourceAdmissionService admissions) {
        this.admissions = admissions;
        this.assets=assets;this.storage=storage;this.jobs=jobs;this.environment=environment;this.enabled=enabled;
        this.prefix=properties.pathPrefix();
    }
    public void requireTestAdmission() {
        var profiles=Set.of(environment.getActiveProfiles());
        if(!enabled || (!profiles.contains("local") && !profiles.contains("test"))
                || profiles.stream().anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"))
                || prefix==null || !prefix.matches("(local|test)(/[A-Za-z0-9_-]+)*"))
            throw new IllegalStateException("NO_QUALIFIED_PRODUCTION_READING_PROFILE");
    }
    @Override public Preparation prepare(Snapshot snapshot,Map<Role,TestProfile> requested) {
        return prepare(snapshot,requested,false);
    }
    /** Uses explicit immutable admission identities while preserving the legacy P2 capture contract. */
    public Preparation prepareAdmitted(Snapshot snapshot,Map<Role,TestProfile> requested) {
        return prepare(snapshot,requested,true);
    }
    private Preparation prepare(Snapshot snapshot,Map<Role,TestProfile> requested,boolean admitted) {
        requireTestAdmission(); snapshot.requireComplete();
        if(!requested.keySet().equals(Set.of(Role.values()))) throw new IllegalArgumentException("ALL_ROLE_PROFILES_REQUIRED");
        var deadline=ReadingDeadline.start(requested.values().stream().mapToLong(TestProfile::revisionMillis).min().orElseThrow());
        try(var capacity=ReadingPreparationCapacity.acquire()) {
            requested.values().forEach(ReadingPreparationCapacity::requireParentMemory);
            var profiles=new EnumMap<Role,Profile>(Role.class);
            requested.forEach((role,profile) -> profiles.put(role,ReadingProfiles.capture(role,profile)));
            UUID captureOwner=UUID.randomUUID();
            var sources=new ArrayList<Source>();
            for(var binding:snapshot.bindings()) {
                deadline.check();
                sources.add(capture(captureOwner,binding,profiles.get(binding.role()).limits(),deadline,admitted));
            }
            for(var source:sources) {
                deadline.check();
                var current=assets.findById(source.binding().assetId()).orElseThrow();
                if(current.getProvider()!=source.provider() || !current.getObjectPath().equals(source.original().path())
                        || !storage.observe(source.provider(),current.getObjectPath(),deadline.remainingMillis(profiles.get(source.binding().role()).limits().sourceMillis())).equals(source.original())) throw new IllegalStateException("SOURCE_BINDING_CHANGED_DURING_CAPTURE");
            }
            deadline.check();
            String sourceSet=ReadingIdentity.hash("tellpal.reading.sources.v1",sources.stream().map(source -> Map.of("binding",source.binding(),"provider",source.provider(),"retained",source.retained(),"sha256",source.sha256(),"width",source.width(),"height",source.height(),"sourceAuthority", source.admissionId()==null?"LEGACY_CAPTURE":source.admissionId()+":"+source.admissionEpoch())).toList());
            var fingerprints=new TreeMap<String,Object>(); profiles.forEach((r,p) -> fingerprints.put(r.name(),p.fingerprint()));
            String profileSet=ReadingIdentity.hash("tellpal.reading.profiles.v1",fingerprints);
            var obligations=sources.stream().map(s -> Map.of("binding",s.binding(),"rendition",rendition(s,profiles.get(s.binding().role())),"sourceAuthority",s.admissionId()==null?"LEGACY_CAPTURE":s.admissionId()+":"+s.admissionEpoch())).toList();
            String revision=ReadingIdentity.hash("tellpal.reading.revision.v1",Map.of("schema",1,"type","STORY","experience","READING",
                    "contentId",snapshot.contentId(),"language",snapshot.language(),"obligations",obligations));
            deadline.check();
            var result=jobs.schedule(new ReadingWork(snapshot,List.copyOf(sources),Map.copyOf(profiles),sourceSet,profileSet,revision),deadline);
            jobs.attachCapture(captureOwner,result.jobId());
            if(result.state()==State.COMPLETED) verifyReady(result,jobs.work(result.jobId()),deadline);
            deadline.check();
            return result;
        } catch(IOException e) { throw new IllegalStateException("READING_CAPTURE_REJECTED:"+e.getMessage(),e); }
    }
    private Source capture(UUID captureOwner,Binding binding,TestProfile profile,ReadingDeadline deadline,boolean admitted) throws IOException {
        var asset=assets.findById(binding.assetId()).orElseThrow(() -> new IllegalArgumentException("SOURCE_ASSET_MISSING"));
        if(asset.getMediaType()!=MediaAssetType.IMAGE || asset.getKind()!=MediaKind.ORIGINAL_IMAGE) throw new IllegalArgumentException("RETAINED_ORIGINAL_IMAGE_REQUIRED");
        var authority=admitted?admissions.require(binding.assetId(),false):null;
        var original=authority==null?storage.observe(asset.getProvider(),asset.getObjectPath(),deadline.remainingMillis(profile.sourceMillis())):admissions.object(authority);
        if(authority!=null && (!asset.getObjectPath().equals(original.path()) || !asset.getProvider().name().equals(authority.get("provider")))) throw new IllegalStateException("ADMISSION_BINDING_MISMATCH");
        byte[] bytes=storage.read(asset.getProvider(),original,profile.maxSourceBytes(),deadline.remainingMillis(profile.sourceMillis()));
        deadline.check();
        var decoded=ReadingImageInspector.decode(bytes,profile);
        deadline.check();
        if(!original.mime().equals(decoded.mime())) throw new IOException("SOURCE_STORAGE_MIME_MISMATCH");
        if (ReadingPreservation.enabled(profile) && decoded.mime().equals("image/webp")) throw new IOException("SOURCE_PNG_OR_JPEG_REQUIRED");
        if(!ReadingPreservation.enabled(profile) && (decoded.image().getWidth()<profile.width() || decoded.image().getHeight()<profile.height())) throw new IOException("SOURCE_SHORTAGE_NO_UPSCALE");
        String sha=ReadingIdentity.sha(bytes);
        if(authority!=null && !sha.equals(authority.get("sha256"))) throw new IllegalStateException("ADMITTED_SOURCE_INTEGRITY_FAILED");
        String path=prefix+"/reading/sources/"+sha;
        jobs.planCapture(captureOwner,path,asset.getProvider().name(),sha);
        var retained=storage.create(asset.getProvider(),path,original.mime(),bytes,profile.maxSourceBytes(),deadline.remainingMillis(profile.sourceMillis()));
        jobs.observeCapture(captureOwner,path,retained.version());
        deadline.check();
        return new Source(binding,asset.getProvider(),original,retained,sha,decoded.image().getWidth(),decoded.image().getHeight(),authority==null?null:(UUID)authority.get("id"),authority==null?null:((Number)authority.get("epoch")).longValue());
    }
    @Override public Preparation find(UUID jobId) {
        return find(jobId,null);
    }
    /** Shares the caller's monotonic budget without changing the legacy public find contract. */
    Preparation find(UUID jobId,ReadingDeadline sharedDeadline) {
        long started=System.nanoTime();
        requireTestAdmission();
        try(var capacity=ReadingPreparationCapacity.acquire()) {
            var result=jobs.find(jobId);var work=jobs.work(jobId);
            var deadline=sharedDeadline==null?ReadingDeadline.since(started,work.profiles().values().stream().mapToLong(p -> p.limits().revisionMillis()).min().orElseThrow()):sharedDeadline;
            work.profiles().values().forEach(p -> ReadingPreparationCapacity.requireParentMemory(p.limits()));
            deadline.check();
            if(result.state()==State.COMPLETED) verifyReady(result,work,deadline);
            deadline.check();return result;
        }
    }
    @Override public Preparation retry(UUID jobId) { requireTestAdmission(); return jobs.retry(jobId); }
    private void verifyReady(Preparation result,ReadingWork work,ReadingDeadline deadline) {
        try {
            deadline.check();
            Manifest manifest=mapper.readValue(result.manifest(),Manifest.class);
            work.requireCompleteManifest(manifest);
            if(!manifest.digest().equals(result.manifestDigest())) throw new IOException("MANIFEST_DIGEST_MISMATCH");
            deadline.check();
            for(int i=0;i<manifest.outputs().size();i++) {
                Output output=manifest.outputs().get(i);var profile=work.profiles().get(output.binding().role()).limits();
                verifyGeometry(work.sources().get(i),output,profile);
                verifyOutput(storage,work.sources().get(i),output,profile,deadline);
            }
            deadline.check();
            jobs.integrity(result.revision(),true,"Fresh pinned stored-object verification");
            deadline.check();
        } catch(IOException | RuntimeException e) {
            if (ReadingVerificationInconclusiveException.causedByResourceRejection(e)) throw new IllegalStateException("READING_VERIFICATION_INCONCLUSIVE", e);
            jobs.integrity(result.revision(),false,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());
            throw new IllegalStateException("READY_INTEGRITY_FAILED",e);
        }
    }
    public static String rendition(Source source,Profile profile) {
        return ReadingWork.rendition(source,profile);
    }
    public static void verifyGeometry(Source source,Output output,TestProfile profile) throws IOException {
        try { ReadingWork.requireGeometry(source,output,profile); }
        catch(IllegalArgumentException mismatch) { throw new IOException(mismatch.getMessage(),mismatch); }
    }
    public static void verifyOutput(ReadingObjectStorage storage,Output output,TestProfile profile) throws IOException {
        verifyOutput(storage,output,profile,ReadingDeadline.start(profile.revisionMillis()));
    }
    public static void verifyOutput(ReadingObjectStorage storage,Output output,TestProfile profile,ReadingDeadline deadline) throws IOException {
        ReadingPreparationCapacity.requireParentMemory(profile);
        byte[] persisted=storage.read(output.provider(),output.stored(),profile.maxOutputBytes(),deadline.remainingMillis(profile.sourceMillis()));
        deadline.check();
        var image=ReadingImageInspector.decode(persisted,profile);
        if(output.width()>profile.width() || output.height()>profile.height() || !output.stored().mime().equals(image.mime()) || (!ReadingPreservation.enabled(profile) && !image.mime().equals("image/png")) || !ReadingIdentity.sha(persisted).equals(output.sha256())
                || image.image().getWidth()!=output.width() || image.image().getHeight()!=output.height()) throw new IOException("PERSISTED_OUTPUT_VERIFICATION_FAILED");
        deadline.check();
    }
    /** Stored çıktı yeni ve bağımsız source read ile tam koruma sözleşmesinden geçer. */
    public static void verifyOutput(ReadingObjectStorage storage, Source source, Output output, TestProfile profile, ReadingDeadline deadline) throws IOException {
        verifyOutput(storage, output, profile, deadline);
        if (!ReadingPreservation.enabled(profile)) return;
        byte[] original = storage.read(source.provider(), source.retained(), profile.maxSourceBytes(), deadline.remainingMillis(profile.sourceMillis()));
        if (!ReadingIdentity.sha(original).equals(source.sha256()) || !ReadingPreservation.mime(original).equals(source.retained().mime()))
            throw new IOException("RETAINED_SOURCE_INTEGRITY_FAILED");
        byte[] persisted = storage.read(output.provider(), output.stored(), profile.maxOutputBytes(), deadline.remainingMillis(profile.sourceMillis()));
        if (!ReadingIdentity.sha(persisted).equals(output.sha256()) || !ReadingPreservation.mime(persisted).equals(output.stored().mime()))
            throw new IOException("PERSISTED_OUTPUT_VERIFICATION_FAILED");
        ReadingPreservation.verify(original, persisted, profile);
        deadline.check();
    }
    public String prefix() { return prefix; }
}
