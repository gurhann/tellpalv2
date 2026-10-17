package com.tellpal.v2.asset.application.reading;

import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import com.tellpal.v2.asset.api.*;
import com.tellpal.v2.asset.api.ReadingImageDeliveryApi.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.domain.reading.ReadingWork.*;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector;
import com.tellpal.v2.asset.infrastructure.storage.*;

/** Storage I/O transaction dışında; bozuk pinned nesne mevcut availability yolunu kapatır. */
@Service
public class ReadingImageDeliveryService implements ReadingImageDeliveryApi {
    private final ReadingJobStore jobs;
    private final ReadingObjectStorage storage;
    private final ReadingComparisonBudget budget;
    private final ObjectMapper mapper = new ObjectMapper();
    public ReadingImageDeliveryService(ReadingJobStore jobs, ReadingObjectStorage storage, ReadingComparisonBudget budget) {
        this.jobs = jobs; this.storage = storage; this.budget=budget;
    }
    @Override public void reserveControl(Limits limits) { budget.reserve(limits,0); }
    @Override
    public Descriptor descriptor(ReadingPublicationReadyApi.Proof proof) {
        var work = jobs.work(proof.jobId());
        var manifest = manifest(proof, work);
        var profiles = new EnumMap<com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role, List<Profile>>(com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.class);
        work.profiles().forEach((role, profile) -> profiles.put(role, List.of(new Profile(profile.fingerprint(),
                profile.limits().width(), profile.limits().height(), profile.limits().allowGrowth()))));
        var entries = new ArrayList<Entry>();
        for (int i = 0; i < work.sources().size(); i++) {
            var source = work.sources().get(i); var output = manifest.outputs().get(i);
            entries.add(new Entry(new ReadingImageDeliveryApi.Source(source.binding(), raster(source.provider().name(), source.retained(), source.sha256(), source.width(), source.height())),
                    List.of(new Rendition(output.renditionKey(), output.sourceSha256(), output.profileFingerprint(), raster(output.provider().name(), output.stored(), output.sha256(), output.width(), output.height())))));
        }
        return new Descriptor(List.copyOf(entries), Map.copyOf(profiles));
    }
    @Override
    public byte[] read(ReadingPublicationReadyApi.Proof proof, Selection selection, Limits limits) {
        ReadingPreparationCapacity.requireNoTransaction();
        var work = jobs.work(proof.jobId());
        var manifest = manifest(proof, work);
        int index = -1;
        for (int i = 0; i < work.sources().size(); i++) {
            var binding = work.sources().get(i).binding();
            if (binding.role() == selection.role() && binding.pageNumber() == selection.pageNumber()) index = i;
        }
        if (index < 0) throw new IllegalArgumentException("UNKNOWN_READING_BINDING");
        var source = work.sources().get(index); var output = manifest.outputs().get(index);
        if (!Objects.equals(selection.renditionKey(), output.renditionKey())) throw new IllegalArgumentException("UNKNOWN_RENDITION");
        var object = selection.canonicalSource() ? source.retained() : output.stored();
        int width = selection.canonicalSource() ? source.width() : output.width();
        int height = selection.canonicalSource() ? source.height() : output.height();
        String sha = selection.canonicalSource() ? source.sha256() : output.sha256();
        if (object.bytes() > limits.maxEncodedBytes() || (long) width * height > limits.maxPixels())
            throw new IllegalStateException("DELIVERY_RESOURCE_LIMIT");
        if (!"image/png".equals(object.mime())) throw new IllegalArgumentException("CANONICAL_PNG_REQUIRED");
        var deadline = ReadingDeadline.start(limits.deadlineMillis());
        boolean completeRead=false;
        try (var capacity = ReadingPreparationCapacity.acquire()) {
            ReadingPreparationCapacity.requireParentMemory(work.profiles().get(selection.role()).limits());
            budget.reserve(limits,object.bytes());
            var bytes = storage.read(source.provider(), object, limits.maxEncodedBytes(), deadline.remainingMillis(limits.deadlineMillis()));
            completeRead=true;
            deadline.check();
            requireActualRaster(bytes, limits.maxPixels());
            var decoded = ReadingImageInspector.decode(bytes, work.profiles().get(selection.role()).limits());
            try {
                if (!object.mime().equals(decoded.mime()) || decoded.image().getWidth() != width
                        || decoded.image().getHeight() != height || !ReadingIdentity.sha(bytes).equals(sha))
                    throw new IOException("PINNED_IMAGE_INTEGRITY");
            } finally { decoded.image().flush(); }
            deadline.check();
            return bytes;
        } catch (IOException | RuntimeException failure) {
            if (ReadingVerificationInconclusiveException.causedByResourceRejection(failure))
                throw new IllegalStateException("DELIVERY_RESOURCE_LIMIT", failure);
            if (!completeRead && !authoritativeStorageFailure(failure))
                throw new IllegalStateException("DELIVERY_TRANSPORT_UNAVAILABLE",failure);
            jobs.integrity(proof.revision(), false, "PINNED_DELIVERY_INTEGRITY_FAILED");
            throw new IllegalStateException("DELIVERY_INTEGRITY_FAILED", failure);
        }
    }
    private static boolean authoritativeStorageFailure(Throwable failure) {
        for(Throwable cause=failure;cause!=null;cause=cause.getCause()) {
            if(cause instanceof com.google.cloud.storage.StorageException storageFailure)
                return storageFailure.getCode()==404;
            if(cause instanceof java.nio.file.NoSuchFileException || cause instanceof java.io.FileNotFoundException) return true;
            if(cause.getMessage()!=null && Set.of("PINNED_STORAGE_METADATA_CHANGED","STORAGE_OBJECT_MISSING","STORAGE_SIZE_MISMATCH").contains(cause.getMessage())) return true;
        }
        return false;
    }
    static void requireActualRaster(byte[] bytes, long maxPixels) throws IOException {
        javax.imageio.ImageIO.setUseCache(false);
        try (var stream=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
            var readers=javax.imageio.ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("INVALID_IMAGE_HEADER");
            var reader=readers.next();
            try {
                reader.setInput(stream);
                if ((long)reader.getWidth(0)*reader.getHeight(0)>maxPixels) throw new IOException("ACTUAL_RASTER_LIMIT");
            } finally { reader.dispose(); }
        }
    }
    private Manifest manifest(ReadingPublicationReadyApi.Proof proof, ReadingWork work) {
        var state = jobs.find(proof.jobId());
        try {
            var manifest = mapper.readValue(state.manifest(), Manifest.class);
            work.requireCompleteManifest(manifest);
            if (!manifest.digest().equals(proof.manifestDigest()) || !work.revision().equals(proof.revision())
                    || !work.snapshot().equals(proof.snapshot())) throw new IllegalArgumentException("PROOF_MISMATCH");
            return manifest;
        } catch (IOException | IllegalArgumentException e) { throw new IllegalStateException("INVALID_DELIVERY_MANIFEST", e); }
    }
    private static Raster raster(String provider, VersionedStorageObject object, String sha, int width, int height) {
        return new Raster(provider, object.path(), object.version(), object.mime(), object.bytes(), sha, width, height);
    }
}
