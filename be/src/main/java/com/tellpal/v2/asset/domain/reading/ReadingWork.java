package com.tellpal.v2.asset.domain.reading;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.VersionedStorageObject;

/** Immutable captured work, independent of mutable content/page/asset row lifetime. */
public record ReadingWork(Snapshot snapshot, List<Source> sources, Map<Role, Profile> profiles,
        String sourceSet, String profileSet, String revision) {
    public ReadingWork {
        sources=List.copyOf(sources);
        profiles=Map.copyOf(profiles);
    }

    /** Compares immutable preparation obligations while allowing a fresh observation of identical original bytes. */
    public boolean sameCapturedIdentity(ReadingWork other) {
        if(!revision.equals(other.revision()) || !sourceSet.equals(other.sourceSet()) || !profileSet.equals(other.profileSet())
                || !snapshot.equals(other.snapshot()) || !profiles.equals(other.profiles()) || sources.size()!=other.sources().size()) return false;
        for(int i=0;i<sources.size();i++) {
            var a=sources.get(i);var b=other.sources().get(i);
            if(!a.binding().equals(b.binding()) || a.provider()!=b.provider() || !a.retained().equals(b.retained())
                    || !a.sha256().equals(b.sha256()) || a.width()!=b.width() || a.height()!=b.height() || !java.util.Objects.equals(a.admissionId(),b.admissionId()) || !java.util.Objects.equals(a.admissionEpoch(),b.admissionEpoch())) return false;
        }
        return true;
    }

    /** Requires the complete ordered role/source/profile obligations before the owning job may become READY. */
    public void requireCompleteManifest(Manifest manifest) {
        if(!revision.equals(manifest.revision()) || !snapshot.equals(manifest.snapshot()) || manifest.outputs().size()!=sources.size())
            throw new IllegalArgumentException("COMPLETE_MANIFEST_REQUIRED");
        for(int i=0;i<sources.size();i++) {
            var source=sources.get(i);var output=manifest.outputs().get(i);var profile=profiles.get(source.binding().role());
            if(!source.binding().equals(output.binding()) || !source.sha256().equals(output.sourceSha256())
                    || !profile.fingerprint().equals(output.profileFingerprint()) || output.width()<=0 || output.height()<=0
                    || output.width()>profile.limits().width() || output.height()>profile.limits().height()
                    || !rendition(source,profile).equals(output.renditionKey()) || output.provider()!=source.provider()
                    || (!profile.limits().allowGrowth() && output.stored().bytes()>=source.retained().bytes())
                    || !output.stored().mime().equals("image/png") || !output.sha256().matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("OUTPUT_OBLIGATION_MISMATCH");
            try { requireGeometry(source,output,profile.limits()); }
            catch(IllegalArgumentException mismatch) { throw new IllegalArgumentException("OUTPUT_GEOMETRY_MISMATCH",mismatch); }
        }
    }

    /** Renditions are reusable only under the same role, actual source bytes and complete captured profile. */
    public static String rendition(Source source,Profile profile) {
        return ReadingIdentity.hash("tellpal.reading.rendition.v1",Map.of("schema",1,"role",source.binding().role(),
                "sourceSha256",source.sha256(),"profileSha256",profile.fingerprint()));
    }

    /** Independently derives the exact floor-fit dimensions from the captured original; source shortage cannot upscale. */
    public static void requireGeometry(Source source,Output output,TestProfile profile) {
        double factor=Math.min((double)profile.width()/source.width(),(double)profile.height()/source.height());
        if(source.width()<profile.width() || source.height()<profile.height()
                || output.width()!=Math.max(1,(int)Math.floor(source.width()*factor))
                || output.height()!=Math.max(1,(int)Math.floor(source.height()*factor)))
            throw new IllegalArgumentException("PROFILE_GEOMETRY_MISMATCH");
    }

    public record Profile(TestProfile limits, String descriptor, String fingerprint) { }
    public record Source(Binding binding, StorageProvider provider, VersionedStorageObject original,
            VersionedStorageObject retained, String sha256, int width, int height, java.util.UUID admissionId, Long admissionEpoch) {
        public Source(Binding binding, StorageProvider provider, VersionedStorageObject original, VersionedStorageObject retained, String sha256,int width,int height) {
            this(binding,provider,original,retained,sha256,width,height,null,null);
        }
    }
    public record Attempt(UUID jobId, int number, UUID lease, long expiresEpochMillis, String sourceSet, String profileSet) { }
    public record Output(Binding binding, String sourceSha256, String profileFingerprint, String renditionKey,
            StorageProvider provider, VersionedStorageObject stored, String sha256, int width, int height) { }
    public record Manifest(String revision, Snapshot snapshot, List<Output> outputs) {
        public Manifest { outputs = List.copyOf(outputs); }
        public String canonical() { return ReadingIdentity.canonical(this); }
        public String digest() { return ReadingIdentity.hash("tellpal.reading.manifest.v1", this); }
    }
}
