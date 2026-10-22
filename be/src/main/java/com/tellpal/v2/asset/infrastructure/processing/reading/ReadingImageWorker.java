package com.tellpal.v2.asset.infrastructure.processing.reading;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.tellpal.v2.asset.application.reading.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;
import com.tellpal.v2.asset.infrastructure.media.reading.*;
import com.tellpal.v2.asset.infrastructure.storage.ReadingObjectStorage;

/** Durable polling/recovery with captured ownership and retained orphan evidence; no publication hooks. */
@Component
public class ReadingImageWorker {
    private final ReadingImagePreparationService admission;
    private final ReadingJobStore jobs;
    private final ReadingObjectStorage storage;
    private final Clock clock;
    private final ReadingImageProcess process;
    @Autowired
    public ReadingImageWorker(ReadingImagePreparationService admission,ReadingJobStore jobs,ReadingObjectStorage storage,Clock clock) {
        this(admission,jobs,storage,clock,new ReadingImageProcess());
    }
    public ReadingImageWorker(ReadingImagePreparationService admission,ReadingJobStore jobs,ReadingObjectStorage storage,Clock clock,ReadingImageProcess process) {
        this.admission=admission;this.jobs=jobs;this.storage=storage;this.clock=clock;this.process=process;
    }
    @Scheduled(fixedDelayString="${tellpal.asset.reading.poll-delay:5000}")
    public void poll() {
        try { admission.requireTestAdmission(); }
        catch(IllegalStateException disabled) { return; }
        jobs.next(clock.millis()).ifPresent(this::run);
    }
    public void run(UUID id) {
        admission.requireTestAdmission();
        long started=System.nanoTime();
        try(var capacity=ReadingPreparationCapacity.acquire()) {
            var optional=jobs.claim(id,clock.millis()); if(optional.isEmpty()) return;
            Attempt token=optional.get();
            try {
                var work=jobs.work(id);
                var deadline=ReadingDeadline.since(started,work.profiles().values().stream().mapToLong(p -> p.limits().revisionMillis()).min().orElseThrow());
                work.profiles().values().forEach(p -> ReadingPreparationCapacity.requireParentMemory(p.limits()));
                var outputs=new ArrayList<Output>();
                for(var source:work.sources()) {
                    deadline.check();
                    var profile=work.profiles().get(source.binding().role());
                    if(!ReadingProfiles.capture(source.binding().role(),profile.limits()).equals(profile)) throw new IllegalStateException("ENCODER_RUNTIME_PROFILE_CHANGED");
                    byte[] original=storage.read(source.provider(),source.retained(),profile.limits().maxSourceBytes(),deadline.remainingMillis(profile.limits().sourceMillis()));
                    if(!ReadingIdentity.sha(original).equals(source.sha256())) throw new IllegalStateException("RETAINED_SOURCE_SHA_MISMATCH");
                    var sourceDecoded = ReadingImageInspector.decode(original, profile.limits());
                    if (!sourceDecoded.mime().equals(source.retained().mime())) throw new IllegalStateException("SOURCE_STORAGE_MIME_MISMATCH");
                    byte[] bytes=process.convert(original,profile.limits(),deadline);
                    if(!profile.limits().allowGrowth() && bytes.length>=original.length) throw new IllegalStateException("OUTPUT_GROWTH_NOT_ADMITTED");
                    var decoded=ReadingImageInspector.decode(bytes,profile.limits());
                    ReadingPreservation.verify(original, bytes, profile.limits());
                    String mime = ReadingPreservation.outputMime(source.retained().mime(), profile.limits());
                    if (!decoded.mime().equals(mime)) throw new IllegalStateException("OUTPUT_POLICY_MIME_MISMATCH");
                    String extension = mime.equals("image/png") ? ".png" : mime.equals("image/jpeg") ? ".jpg" : ".webp";
                    String key=ReadingImagePreparationService.rendition(source,profile);
                    String outputPath=admission.prefix()+"/reading/renditions/"+key+extension;
                    jobs.evidence(token,"PLANNED_OBJECT",outputPath);
                    var stored=storage.create(source.provider(),outputPath,mime,bytes,profile.limits().maxOutputBytes(),deadline.remainingMillis(profile.limits().sourceMillis()));
                    jobs.evidence(token,"RETAINED_OBJECT",stored.path()+"@"+stored.version());
                    var output=new Output(source.binding(),source.sha256(),profile.fingerprint(),key,source.provider(),stored,ReadingIdentity.sha(bytes),decoded.image().getWidth(),decoded.image().getHeight());
                    ReadingImagePreparationService.verifyGeometry(source,output,profile.limits());
                    ReadingImagePreparationService.verifyOutput(storage,source,output,profile.limits(),deadline);
                    outputs.add(output);
                }
                deadline.check();
                jobs.succeed(token,new Manifest(work.revision(),work.snapshot(),outputs),clock.millis(),deadline);
            } catch(Exception failure) {
                String diagnostic=failure.getClass().getSimpleName()+":"+Objects.toString(failure.getMessage(),"failure");
                if(diagnostic.length()>4096) diagnostic=diagnostic.substring(0,4096);
                jobs.evidence(token,"ATTEMPT_FAILURE",diagnostic);
                try { jobs.fail(token,diagnostic,clock.millis()); }
                catch(ReadingAttemptRejectedException stale) { jobs.evidence(token,"FENCED_CALLBACK",stale.getMessage()); }
            }
        }
    }
}
