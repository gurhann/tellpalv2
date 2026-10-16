package com.tellpal.v2.asset.domain.reading;

import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;

/** Captures every engine setting including concrete codec/provider/runtime and synthetic admission. */
public final class ReadingProfiles {
    private ReadingProfiles() { }
    private static final class RuntimeEvidence {
        private static final Map<String,Object> VALUE=runtimeEvidence();
    }
    private static Map<String,Object> runtimeEvidence() {
        var evidence=new TreeMap<String,Object>();
        var home=java.nio.file.Path.of(System.getProperty("java.home"));
        for(String name:java.util.List.of("bin/java","lib/modules","lib/libjava.dylib","lib/libjava.so","lib/libawt.dylib","lib/libawt.so","lib/server/libjvm.dylib","lib/server/libjvm.so")) {
            var file=home.resolve(name);
            if(java.nio.file.Files.isRegularFile(file)) {
                try(var input=java.nio.file.Files.newInputStream(file)) { evidence.put(name,digest(input)); }
                catch(java.io.IOException e) { throw new IllegalStateException("Runtime binary identity unavailable",e); }
            }
        }
        if(!evidence.containsKey("bin/java") || !evidence.containsKey("lib/modules")) throw new IllegalStateException("Runtime binary identity unavailable");
        for(Class<?> type:java.util.List.of(ReadingProfiles.class,ReadingIdentity.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageChild.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess.class)) {
            try(var input=type.getResourceAsStream("/"+type.getName().replace('.','/')+".class")) {
                if(input==null) throw new IllegalStateException("Engine class identity unavailable");
                evidence.put(type.getName(),digest(input));
            } catch(java.io.IOException e) { throw new IllegalStateException("Engine class identity unavailable",e); }
        }
        for(String name:java.util.List.of("com.tellpal.v2.asset.infrastructure.storage.FirebaseStorageAssetClient",
                "com.tellpal.v2.asset.infrastructure.storage.FirebaseStorageAssetClient$1",
                "com.google.cloud.storage.StorageRetryStrategy",
                "com.google.cloud.storage.DefaultStorageRetryStrategy",
                "com.google.cloud.storage.DefaultStorageRetryStrategy$InterceptorImpl",
                com.google.cloud.storage.StorageRetryStrategy.getDefaultStorageRetryStrategy().getNonidempotentHandler().getClass().getName())) {
            try(var input=ReadingProfiles.class.getResourceAsStream("/"+name.replace('.','/')+".class")) {
                if(input==null) throw new IllegalStateException("Storage retry implementation identity unavailable");
                evidence.put(name,digest(input));
            } catch(java.io.IOException e) { throw new IllegalStateException("Storage retry implementation identity unavailable",e); }
        }
        return Map.copyOf(evidence);
    }
    private static String digest(java.io.InputStream input) throws java.io.IOException {
        try {
            var digest=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[65536];int count;
            while((count=input.read(buffer))!=-1) digest.update(buffer,0,count);
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static ReadingWork.Profile capture(Role role, TestProfile limits) {
        var writers = ImageIO.getImageWritersByFormatName("PNG");
        if (!writers.hasNext()) throw new IllegalStateException("PNG writer unavailable");
        var writer = writers.next();
        var provider = writer.getOriginatingProvider();
        var descriptor = new TreeMap<String,Object>();
        descriptor.put("childProviderHandshake","FULL_DESCRIPTOR_USING_PAGE_ROLE"); descriptor.put("maxObligations",102); descriptor.put("schema",1); descriptor.put("engine","tellpal-reading-child-v1"); descriptor.put("admission","LOCAL_TEST_ONLY");
        descriptor.put("role",role); descriptor.put("limits",limits);
        descriptor.put("runtime", Map.of("java",System.getProperty("java.runtime.version"),"vendor",System.getProperty("java.vendor"),
                "vm",System.getProperty("java.vm.name"),"os",System.getProperty("os.name"),"arch",System.getProperty("os.arch")));
        descriptor.put("runtimeAndEngineBinaries",RuntimeEvidence.VALUE);
        descriptor.put("encoder",Map.of("class",writer.getClass().getName(),"provider",provider.getClass().getName(),"version",provider.getVersion(),
                "format","PNG","compressionMode","MODE_EXPLICIT","compressionType","Deflate","compressionQuality","1.0",
                "metadata","NONE","progressive","DISABLED"));
        descriptor.put("transform",Map.of("fit","EXACT_ASPECT_FIT_FLOOR","sourceMinimum","TARGET_BOX_BOTH_AXES","upscale",false,"resampler","JAVA2D_BICUBIC",
                "rendering","QUALITY","colorRendering","QUALITY","outputType","TYPE_INT_RGB","directFromOriginal",true));
        descriptor.put("durationAdmission","SHARED_MONOTONIC_REVISION_WITH_REMAINING_OPERATION_CAP");
        descriptor.put("parentAdmission",Map.of("maxHeapDivisor",4,"availableHeapDivisor",2,"scope","PARENT_ONLY","reservation","DECLARED_PARENT_MEMORY_BYTES"));
        descriptor.put("storage",Map.of("rpcTimeoutMillis",limits.sourceMillis(),"connectTimeoutMillis",limits.sourceMillis(),
                "readTimeoutMillis",limits.sourceMillis(),"maxAttempts",1,"readChunkBytes",1048576,
                "retryPolicy","SDK_DEFAULT_NONIDEMPOTENT_HANDLER_FOR_BOTH_CLASSIFICATIONS",
                "transportTimeoutSemantics","SOCKET_INACTIVITY_WITH_BETWEEN_CALL_MONOTONIC_CHECKS",
                "absoluteInFlightDeadline","DEFERRED_UNQUALIFIED_SLOW_TRICKLE"));
        descriptor.put("source", Map.of("formats",java.util.List.of("RGB8_NONINTERLACED_PNG","BASELINE_JFIF_RGB8_JPEG"),"frames",1,"color","SRGB_ONLY",
                "bits",8,"alpha","FORBIDDEN","orientation","IDENTITY_ONLY","metadata","STRICT_CONTAINER_ALLOWLIST",
                "metadataBytes",65536,"crc","REQUIRED","trailingBytes","FORBIDDEN"));
        descriptor.put("rssExitConfirmationMillis",com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess.RSS_EXIT_CONFIRMATION_MILLIS);
        descriptor.put("jvm",Map.of("activeProcessors",1,"gc","SerialGC","metaspaceMiB",64,"codeCacheMiB",32,"stackKiB",512,
                "headless",true,"imageIoCache",false,"monitorMillis",20,"rssProbe","linux-proc-status-or-macos-ps-rss-kib","termination","FORCE_AND_WAIT"));
        writer.dispose();
        return new ReadingWork.Profile(limits, ReadingIdentity.canonical(descriptor), ReadingIdentity.hash("tellpal.reading.profile.v1",descriptor));
    }
}
