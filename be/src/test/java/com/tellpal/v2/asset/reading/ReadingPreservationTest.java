package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.media.reading.*;
import com.tellpal.v2.asset.application.reading.ReadingImagePreparationService;
import com.tellpal.v2.asset.domain.StorageProvider;

class ReadingPreservationTest {
    @TempDir Path directory;
    static TestProfile profile() {
        var p = ReadingFixtures.profile();
        return new TestProfile(100, 100, p.maxSourceBytes(), p.maxOutputBytes(), p.maxPixels(), p.parentMemoryBytes(),
                p.heapMiB(), p.directMiB(), p.processMiB(), p.diskBytes(), p.sourceMillis(), p.revisionMillis(), 1, 1,
                true, "Yalniz yerel sentetik growth kaniti", FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
    }
    static void tools() { ReadingNativeTestTools.require(); }
    @Test void legacyConstructorAndMissingOrNullJsonPolicyKeepPngFit() throws Exception {
        var mapper = new ObjectMapper(); var p = ReadingFixtures.profile();
        assertThat(p.formatPolicy()).isEqualTo(FormatPolicy.PNG_FIT);
        var tree = mapper.valueToTree(p); ((com.fasterxml.jackson.databind.node.ObjectNode) tree).remove("formatPolicy");
        assertThat(mapper.treeToValue(tree, TestProfile.class)).isEqualTo(p);
        ((com.fasterxml.jackson.databind.node.ObjectNode) tree).putNull("formatPolicy");
        assertThat(mapper.treeToValue(tree, TestProfile.class)).isEqualTo(p);
        assertThat(mapper.readValue(mapper.writeValueAsBytes(profile()), TestProfile.class)).isEqualTo(profile());
    }
    @Test void realChildCreatesNewWebpAndCopiesJpegWithExactRawProof() throws Exception {
        tools();
        for (String format : List.of("PNG", "JPEG")) {
            byte[] original = ReadingFixtures.quadrants(format);
            var process = new ReadingImageProcess(); byte[] result = process.convert(original, profile());
            assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            assertThat(ReadingPreservation.mime(result)).isEqualTo(format.equals("PNG") ? "image/webp" : "image/jpeg");
            ReadingPreservation.verify(original, result, profile());
            assertThat(ReadingImageInspector.decode(result, profile()).image().getWidth()).isEqualTo(80);
            if (format.equals("JPEG")) assertThat(result).isEqualTo(original);
        }
        assertThat(ReadingProfiles.capture(Role.READING_PAGE, profile()).descriptor()).contains("cwebp", "webpmux", "1.6.0", "-lossless -exact -q 100 -m 6", "VP8L_CEIL_CONSUMED_BITS", ".jar", "ReadingPreservationMetadata$1");
    }
    @Test void transparentAlphaAnimationOrientationUnknownIccAndTrailingAreRejected() throws Exception {
        byte[] original = ReadingFixtures.quadrants("PNG");
        var alpha = new java.awt.image.BufferedImage(80, 60, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var output = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(alpha, "PNG", output);
        assertThatThrownBy(() -> ReadingImageInspector.decode(output.toByteArray(), profile())).hasMessageContaining("TRANSPARENT_PIXEL");
        for (String type : List.of("acTL", "tRNS")) {
            byte[] malformed = insert(original, type, new byte[8]);
            assertThatThrownBy(() -> ReadingImageInspector.decode(malformed, profile())).hasMessageContaining("PNG_CHUNK_REFUSED");
        }
        byte[] orientation = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN).put(new byte[]{'I','I'}).putShort((short)42).putInt(8)
                .putShort((short)1).putShort((short)274).putShort((short)3).putInt(1).putInt(1).putInt(0).array();
        assertThatThrownBy(() -> ReadingImageInspector.decode(insert(original, "eXIf", orientation), profile())).hasMessageContaining("ORIENTATION_REFUSED");
        var compressed = new ByteArrayOutputStream(); try(var zip = new java.util.zip.DeflaterOutputStream(compressed)) { zip.write(new byte[536]); }
        var body = new ByteArrayOutputStream(); body.write(new byte[]{'P',0,0}); body.writeBytes(compressed.toByteArray());
        assertThatThrownBy(() -> ReadingImageInspector.decode(insert(original, "iCCP", body.toByteArray()), profile())).hasMessageContaining("ICC_REFUSED");
        byte[] trailing = Arrays.copyOf(original, original.length + 1);
        assertThatThrownBy(() -> ReadingImageInspector.decode(trailing, profile())).hasMessageContaining("PNG_END_OR_TRAILING");
        assertThatThrownBy(() -> ReadingImageInspector.decode(ReadingFixtures.image(101, 10), profile())).hasMessageContaining("PRESERVATION_GEOMETRY_LIMIT");
    }
    @Test void webpPayloadTrailingDataAndPixelMutationCannotPass() throws Exception {
        tools(); byte[] source = ReadingFixtures.quadrants("PNG");
        byte[] result = new ReadingImageProcess().convert(source, profile());
        assertThatThrownBy(() -> ReadingPreservation.verify(ReadingFixtures.image(80,60), result, profile())).hasMessageContaining("RGB_MISMATCH");
        int length = ByteBuffer.wrap(result, 16, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] malformed = Arrays.copyOf(result, result.length + 2);
        ByteBuffer.wrap(malformed).order(ByteOrder.LITTLE_ENDIAN).putInt(4, malformed.length - 8).putInt(16, length + 2);
        assertThatThrownBy(() -> ReadingImageInspector.decode(malformed, profile())).hasMessageContaining("WEBP_PAYLOAD_TRAILING_OR_TRUNCATED");
    }
    @Test void storedRereadBindsSourceEvenIfCandidateShaAndDimensionsAreValid() throws Exception {
        tools(); var p = profile(); var provider = StorageProvider.FIREBASE_STORAGE;
        var disk = new ReadingFixtures.DiskStorage(directory); var storage = disk.storage();
        byte[] original = ReadingFixtures.quadrants("PNG"), candidate = new ReadingImageProcess().convert(ReadingFixtures.image(80,60), p);
        var sourceObject = storage.create(provider, "original", "image/png", original, p.maxSourceBytes());
        var stored = storage.create(provider, "output", "image/webp", candidate, p.maxOutputBytes());
        var binding = ReadingFixtures.snapshot().bindings().getFirst();
        var source = new ReadingWork.Source(binding, provider, sourceObject, sourceObject, ReadingIdentity.sha(original), 80,60);
        var output = new ReadingWork.Output(binding, source.sha256(), "a".repeat(64), "b".repeat(64), provider, stored, ReadingIdentity.sha(candidate), 80,60);
        assertThatThrownBy(() -> ReadingImagePreparationService.verifyOutput(storage, source, output, p, ReadingDeadline.start(p.revisionMillis()))).hasMessageContaining("RGB_MISMATCH");
    }
    @Test void missingOrChangedToolsFailClosedAndLegacyNeedsNoTools() throws Exception {
        String previous = System.getProperty(ReadingWebpTools.PROPERTY);
        String oldSupervisor = System.getProperty(ReadingProcessSupervisor.PROPERTY);
        try {
            System.setProperty(ReadingWebpTools.PROPERTY, directory.toString());
            System.setProperty(ReadingProcessSupervisor.PROPERTY, directory.toString());
            assertThatThrownBy(() -> ReadingProfiles.capture(Role.READING_PAGE, profile())).hasMessageContaining("WEBP_TOOLS_UNAVAILABLE");
            Files.writeString(directory.resolve("cwebp"), "broken"); directory.resolve("cwebp").toFile().setExecutable(true);
            assertThatThrownBy(() -> ReadingWebpTools.binary("cwebp")).hasMessageContaining("WEBP_BINARY_IDENTITY_MISMATCH");
            assertThat(new ReadingImageProcess().convert(ReadingFixtures.quadrants("PNG"), ReadingFixtures.profile())).isNotEmpty();
        } finally {
            if (previous == null) System.clearProperty(ReadingWebpTools.PROPERTY); else System.setProperty(ReadingWebpTools.PROPERTY, previous);
            if (oldSupervisor == null) System.clearProperty(ReadingProcessSupervisor.PROPERTY); else System.setProperty(ReadingProcessSupervisor.PROPERTY, oldSupervisor);
        }
    }
    @Test void preservationSourcePixelAndMetadataAllocationsAreBounded() throws Exception {
        byte[] original=ReadingFixtures.quadrants("PNG");
        assertThatThrownBy(()->ReadingImageInspector.decode(insert(original,"iTXt",new byte[65_537]),profile())).hasMessageContaining("METADATA_LIMIT");
        var compressed=new ByteArrayOutputStream();try(var zip=new java.util.zip.DeflaterOutputStream(compressed)) { zip.write(new byte[100_000]); }
        var body=new ByteArrayOutputStream();body.writeBytes(new byte[]{'P',0,0});body.writeBytes(compressed.toByteArray());
        assertThatThrownBy(()->ReadingImageInspector.decode(insert(original,"iCCP",body.toByteArray()),profile())).hasMessageContaining("DECOMPRESSED_LIMIT");
        var p=profile();
        var pixelLimit=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),10,p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,p.growthEvidence(),p.formatPolicy());
        assertThatThrownBy(()->ReadingImageInspector.decode(original,pixelLimit)).hasMessageContaining("PIXEL_LIMIT");
        var process=new ReadingImageProcess();
        assertThatThrownBy(()->process.convert(new byte[(int)p.maxSourceBytes()+1],p)).hasMessageContaining("SOURCE_BYTE_LIMIT");
        assertThat(process.lastPid()).isZero();
    }
    @Test void xmpAndNormalizedTiffMustSurviveByteForByte() throws Exception {
        tools();byte[] plain=ReadingFixtures.quadrants("PNG"), plainCandidate=new ReadingImageProcess().convert(plain,profile());
        var xmpBody=new ByteArrayOutputStream();xmpBody.writeBytes("XML:com.adobe.xmp".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        xmpBody.writeBytes(new byte[5]);xmpBody.writeBytes("<x:xmpmeta xmlns:x='adobe:ns:meta/'/>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] tiff=ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN).put(new byte[]{'I','I'}).putShort((short)42).putInt(8)
                .putShort((short)1).putShort((short)305).putShort((short)2).putInt(4).put(new byte[]{'a','b','c',0}).putInt(0).array();
        for(String kind:List.of("iTXt","eXIf")) {
            byte[] withMetadata=insert(plain,kind,kind.equals("iTXt")?xmpBody.toByteArray():tiff);
            byte[] result=new ReadingImageProcess().convert(withMetadata,profile());ReadingPreservation.verify(withMetadata,result,profile());
            assertThatThrownBy(()->ReadingPreservation.verify(withMetadata,plainCandidate,profile())).hasMessageContaining(kind.equals("iTXt")?"XMP_MISMATCH":"TIFF_MISMATCH");
        }
    }
    @Test void deadlineAndTotalNativeRssDiskLogLimitsObserveEntireProcessTreeExit() throws Exception {
        tools();
        for (String mode : List.of("timeout", "rss", "disk", "log")) {
            var p=profile();
            var limits=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),
                    p.heapMiB(),p.directMiB(),mode.equals("rss")?192:p.processMiB(),p.diskBytes(),mode.equals("timeout")?1500:5000,p.revisionMillis(),1,1,true,p.growthEvidence(),p.formatPolicy());
            Path childPid=directory.resolve(mode+".pid");
            var observedRss=new HashMap<Long,Long>();
            var process=new ReadingImageProcess() {
                @Override protected long probeRssKiB(long pid) throws IOException,InterruptedException {
                    long rss=super.probeRssKiB(pid);observedRss.merge(pid,rss,Math::max);return rss;
                }
                @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile ignored) throws IOException {
                    return startOwned(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx64m","-XX:+UseSerialGC","-XX:ReservedCodeCacheSize=16m","-XX:MaxMetaspaceSize=32m","-cp",System.getProperty("java.class.path"),NativeResourceChild.class.getName(),mode,childPid.toString(),output.toString()),log);
                }
            };
            String expected=switch(mode) { case "timeout"->"CHILD_TIMEOUT";case "rss"->"PROCESS_RSS_LIMIT";case "disk"->"TEMP_DISK_LIMIT";default->"CHILD_LOG_LIMIT"; };
            assertThatThrownBy(()->process.convert(ReadingFixtures.quadrants("PNG"),limits)).hasMessageContaining(expected);
            assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            assertThat(Files.exists(childPid)).as(mode+" native PID").isTrue();
            long nativePid=Long.parseLong(Files.readString(childPid));
            long workerPid=Long.parseLong(Files.readString(Path.of(childPid+".worker")));
            assertThat(ProcessHandle.of(nativePid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            assertThat(ProcessHandle.of(workerPid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            if(mode.equals("rss")) {
                assertThat(observedRss.get(workerPid)).isPositive().isLessThan(192*1024L);
                assertThat(observedRss.get(nativePid)).isPositive();
                assertThat(observedRss.values().stream().mapToLong(Long::longValue).sum()).isGreaterThan(192*1024L);
            }
        }
    }
    public static class NativeResourceChild {
        public static void main(String[] args) throws Exception {
            Files.writeString(Path.of(args[1]+".worker"),Long.toString(ProcessHandle.current().pid()));
            Process nativeChild=args[0].equals("rss")
                    ? new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx256m","-cp",System.getProperty("java.class.path"),AllocationChild.class.getName(),args[1]+".allocation-ready").start()
                    : new ProcessBuilder("/bin/sleep","10").start();
            Files.writeString(Path.of(args[1]),Long.toString(nativeChild.pid()));
            if(args[0].equals("rss")) Files.writeString(Path.of(args[1]+".allocation-ready"),"ready");
            if(args[0].equals("disk")) Files.write(Path.of(args[2]),new byte[4*1024*1024]);
            if(args[0].equals("log")) { System.out.write(new byte[(int)ReadingImageProcess.LOG_BYTES+1]);System.out.flush(); }
            nativeChild.waitFor();
        }
    }
    public static class AllocationChild {
        public static void main(String[] args) throws Exception {
            while(!Files.exists(Path.of(args[0]))) Thread.sleep(5);
            byte[] bytes=new byte[140*1024*1024];Arrays.fill(bytes,(byte)1);Thread.sleep(10000);System.out.println(bytes[0]);
        }
    }
    @Test void xmpOrientationNamespaceAliasesAttributesElementsAndExternalEntitiesAreRefused() throws Exception {
        byte[] plain=ReadingFixtures.quadrants("PNG");
        for(String prefix:List.of("tiff","alias")) for(String property:List.of("attribute","element")) for(int orientation:List.of(1,6)) {
            String start="<r xmlns:"+prefix+"='http://ns.adobe.com/tiff/1.0/'";
            String xml=property.equals("attribute") ? start+" "+prefix+":Orientation='"+orientation+"'/>"
                    : start+"><"+prefix+":Orientation>"+orientation+"</"+prefix+":Orientation></r>";
            assertThatThrownBy(()->ReadingImageInspector.decode(insert(plain,"iTXt",xmp(xml)),profile())).hasMessageContaining("ORIENTATION_REFUSED");
        }
        for(String xml:List.of("<!DOCTYPE r [<!ENTITY local SYSTEM 'file:///etc/passwd'>]><r>&local;</r>",
                "<!DOCTYPE r SYSTEM 'https://127.0.0.1:9/no-network'><r/>"))
            assertThatThrownBy(()->ReadingImageInspector.decode(insert(plain,"iTXt",xmp(xml)),profile())).hasMessageContaining("XMP_XML_REFUSED");
        assertThat(ReadingImageInspector.decode(insert(plain,"iTXt",xmp("<r xmlns:t='http://ns.adobe.com/tiff/1.0/' t:Make='preserved'/>")),profile()).image().getWidth()).isEqualTo(80);
    }
    @Test void squarePhysicalDensityIsAcceptedAndNonsquarePixelAspectIsRefused() throws Exception {
        byte[] plain=ReadingFixtures.quadrants("PNG");
        for(int unit:List.of(0,1)) {
            byte[] equal=ByteBuffer.allocate(9).putInt(3000).putInt(3000).put((byte)unit).array();
            assertThat(ReadingImageInspector.decode(insert(plain,"pHYs",equal),profile()).image().getWidth()).isEqualTo(80);
            byte[] different=ByteBuffer.allocate(9).putInt(3000).putInt(6000).put((byte)unit).array();
            assertThatThrownBy(()->ReadingImageInspector.decode(insert(plain,"pHYs",different),profile())).hasMessageContaining("PNG_NONSQUARE_PIXEL_ASPECT_REFUSED");
        }
    }
    static byte[] xmp(String xml) throws IOException {
        var body=new ByteArrayOutputStream();body.writeBytes("XML:com.adobe.xmp".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        body.writeBytes(new byte[5]);body.writeBytes(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));return body.toByteArray();
    }
    static byte[] insert(byte[] original, String type, byte[] body) throws IOException {
        var chunk = new ByteArrayOutputStream(); var output = new DataOutputStream(chunk); byte[] name = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        output.writeInt(body.length); output.write(name); output.write(body); var crc = new CRC32(); crc.update(name); crc.update(body); output.writeInt((int)crc.getValue());
        var result = new ByteArrayOutputStream(); result.write(original,0,33); result.writeBytes(chunk.toByteArray()); result.write(original,33,original.length-33); return result.toByteArray();
    }
}
