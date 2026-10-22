package com.tellpal.v2.asset.domain.reading;

import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;

/** Captures every engine setting including concrete codec/provider/runtime and synthetic admission. */
public final class ReadingProfiles {
    /** PNG writer'da 0.0 en güçlü kayıpsız DEFLATE ayarıdır; 1.0 sıkıştırmayı kapatır. */
    public static final float PNG_COMPRESSION_QUALITY=0.0f;
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
        for(Class<?> type:java.util.List.of(ReadingProfiles.class,ReadingIdentity.class,ReadingWork.class,TestProfile.class,FormatPolicy.class,
                com.tellpal.v2.asset.infrastructure.processing.reading.ReadingImageWorker.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageChild.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageInspector.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingPreservation.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingWebpTools.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess.class,
                com.tellpal.v2.asset.infrastructure.media.reading.ReadingProcessSupervisor.class,
                com.tellpal.v2.asset.application.reading.ReadingSourceAdmissionService.class,
                com.tellpal.v2.asset.application.reading.ReadingImagePreparationService.class,
                com.tellpal.v2.asset.application.reading.ReadingPublicationReadyService.class,
                com.tellpal.v2.asset.application.reading.ReadingPreparationCapacity.class,
                com.tellpal.v2.asset.application.reading.ReadingVerificationInconclusiveException.class,
                com.tellpal.v2.asset.infrastructure.persistence.reading.JdbcReadingSourceAdmissionStore.class,
                com.tellpal.v2.asset.infrastructure.storage.ReadingSourceNamespace.class)) {
            captureClass(type, evidence);
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
        for (String name : java.util.List.of("ReadingPreservationEncoder", "ReadingPreservationRawDecoder", "ReadingPreservationMetadata", "ReadingPreservationMetadata$1", "ReadingPreservationContainers", "ReadingPreservationPng", "ReadingPreservationWebp")) {
            try { captureClass(Class.forName("com.tellpal.v2.asset.infrastructure.media.reading." + name), evidence); }
            catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
        }
        for (String name : java.util.List.of("com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi", "com.twelvemonkeys.imageio.ImageReaderBase", "com.twelvemonkeys.imageio.metadata.AbstractDirectory", "com.twelvemonkeys.lang.Validate", "com.twelvemonkeys.io.LittleEndianDataInputStream", "com.twelvemonkeys.image.ImageUtil")) {
            try {
                Class<?> type = Class.forName(name);
                var location = type.getProtectionDomain().getCodeSource().getLocation();
                if (location.getProtocol().equals("file")) {
                    try (var input = location.openStream()) { evidence.put(name + ".jar", digest(input)); }
                } else {
                    var connection = (java.net.JarURLConnection) type.getResource("/" + name.replace('.', '/') + ".class").openConnection();
                    try (var input = connection.getJarFileURL().openStream()) { evidence.put(name + ".jar", digest(input)); }
                }
            } catch (Exception e) { throw new IllegalStateException("Decoder JAR identity unavailable", e); }
        }
        return Map.copyOf(evidence);
    }
    private static void captureClass(Class<?> type, Map<String,Object> evidence) {
        try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (input == null) throw new IllegalStateException("Engine class identity unavailable");
            evidence.put(type.getName(), digest(input));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        for (Class<?> nested : type.getDeclaredClasses()) captureClass(nested, evidence);
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
        descriptor.put("childProviderHandshake","FULL_DESCRIPTOR_USING_PAGE_ROLE"); descriptor.put("maxObligations",102); descriptor.put("schema",2); descriptor.put("engine","tellpal-reading-child-v2"); descriptor.put("admission","LOCAL_TEST_ONLY");
        descriptor.put("role",role); descriptor.put("limits",limits);
        descriptor.put("runtime", Map.of("java",System.getProperty("java.runtime.version"),"vendor",System.getProperty("java.vendor"),
                "vm",System.getProperty("java.vm.name"),"os",System.getProperty("os.name"),"arch",System.getProperty("os.arch")));
        descriptor.put("runtimeAndEngineBinaries",RuntimeEvidence.VALUE);
        descriptor.put("encoder",Map.of("class",writer.getClass().getName(),"provider",provider.getClass().getName(),"version",provider.getVersion(),
                "format","PNG","compressionMode","MODE_EXPLICIT","compressionType","Deflate","compressionQuality",Float.toString(PNG_COMPRESSION_QUALITY),
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
        descriptor.put("sourceAuthority","CAPTURED_SOURCE_ADMISSION_ID_EPOCH_OR_LEGACY_P2");
        descriptor.put("source", Map.of("formats",java.util.List.of("RGB8_NONINTERLACED_PNG","BASELINE_JFIF_RGB8_JPEG"),"frames",1,"color","SRGB_ONLY",
                "bits",8,"alpha","FORBIDDEN","orientation","IDENTITY_ONLY","metadata","STRICT_CONTAINER_ALLOWLIST",
                "metadataBytes",65536,"crc","REQUIRED","trailingBytes","FORBIDDEN"));
        if (limits.formatPolicy() == FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP) {
            descriptor.put("canonicalPng", descriptor.get("encoder"));
            descriptor.put("encoder", Map.of("tools", com.tellpal.v2.asset.infrastructure.media.reading.ReadingWebpTools.evidence(),
                    "jpeg", "EXACT_BYTE_COPY", "webp", "-lossless -exact -q 100 -m 6 -metadata none", "threads", 1,
                    "input", "RAW_RGB_CANONICAL_PNG_NO_PROFILE", "metadata", "WEBPMUX_ICC_EXIF_XMP_BYTES"));
            descriptor.put("transform", Map.of("geometry", "SOURCE_DIMENSIONS_WITH_TARGET_UPPER_BOUND", "resize", false,
                    "colorConversion", false, "orientationCorrection", false, "rawRGB", "LOGICAL_RGB8_BANDS"));
            descriptor.put("source", Map.of("formats", java.util.List.of("RGB8_OR_OPAQUE_RGBA8_NONINTERLACED_PNG", "BASELINE_RGB8_JPEG"),
                    "alpha", "FULLY_OPAQUE_255_ONLY", "frames", 1, "icc", "NONE_OR_20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab",
                    "metadata", "EXACT_ICC_XMP_NORMALIZED_TIFF", "metadataBytes", 65536));
            descriptor.put("preservation", Map.of("decoder", "imageio-webp:3.15.3/raw-destination/logical-rgb",
                    "webpConsumption", "VP8L_CEIL_CONSUMED_BITS_EQUALS_PAYLOAD_END", "storedReread", "SOURCE_BOUND_FULL_RGB_METADATA",
                    "logBytes", 1048576, "rss", "JAVA_PLUS_NATIVE_DESCENDANTS"));
        }
        descriptor.put("logBytes", com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess.LOG_BYTES);
        descriptor.put("processTree", limits.formatPolicy() == FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP
                ? com.tellpal.v2.asset.infrastructure.media.reading.ReadingProcessSupervisor.evidence() : "SINGLE_JAVA_CHILD_FORCE_AND_WAIT_EXIT");
        descriptor.put("rssExitConfirmationMillis",com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess.RSS_EXIT_CONFIRMATION_MILLIS);
        descriptor.put("jvm",Map.of("activeProcessors",1,"gc","SerialGC","metaspaceMiB",64,"codeCacheMiB",32,"stackKiB",512,
                "headless",true,"imageIoCache",false,"monitorMillis",20,"rssProbe","linux-proc-status-or-macos-ps-rss-kib","termination","FORCE_AND_WAIT"));
        writer.dispose();
        return new ReadingWork.Profile(limits, ReadingIdentity.canonical(descriptor), ReadingIdentity.hash("tellpal.reading.profile.v1",descriptor));
    }
}
