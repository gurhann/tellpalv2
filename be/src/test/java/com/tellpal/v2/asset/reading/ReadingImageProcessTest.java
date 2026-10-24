package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.infrastructure.media.reading.*;
import com.tellpal.v2.asset.infrastructure.storage.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ReadingImageProcessTest {
    @TempDir Path directory;
    @Test void realChildPreservesEveryRgbPixelAndCompressesAgainstUncompressedControl() throws Exception {
        byte[] original=ReadingFixtures.quadrants("PNG");
        var p=ReadingFixtures.profile();
        var profile=new TestProfile(80,60,p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,p.growthEvidence());
        var expected=javax.imageio.ImageIO.read(new ByteArrayInputStream(original));
        var writer=javax.imageio.ImageIO.getImageWritersByFormatName("PNG").next();
        var control=new ByteArrayOutputStream();
        try(var stream=javax.imageio.ImageIO.createImageOutputStream(control)) {
            writer.setOutput(stream);var parameters=writer.getDefaultWriteParam();
            parameters.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionType("Deflate");parameters.setCompressionQuality(1.0f);
            parameters.setProgressiveMode(javax.imageio.ImageWriteParam.MODE_DISABLED);
            writer.write(null,new javax.imageio.IIOImage(expected,null,null),parameters);
        } finally { writer.dispose(); }
        var process=new ReadingImageProcess();byte[] output=process.convert(original,profile);
        Path persisted=directory.resolve("compressed.png");Files.write(persisted,output);
        var actual=javax.imageio.ImageIO.read(persisted.toFile());
        assertThat(actual.getWidth()).isEqualTo(80);assertThat(actual.getHeight()).isEqualTo(60);
        assertThat(actual.getRGB(0,0,80,60,null,0,80)).isEqualTo(expected.getRGB(0,0,80,60,null,0,80));
        assertThat(output.length).isLessThan(control.size()/4);
        assertThat(ReadingProfiles.capture(Role.READING_PAGE,profile).descriptor()).contains("\"compressionQuality\":\"0.0\"");
        assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();

        var bounded=new TestProfile(80,60,p.maxSourceBytes(),output.length-1,p.maxPixels(),p.parentMemoryBytes(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,p.growthEvidence());
        assertThatThrownBy(() -> process.convert(original,bounded)).hasMessageContaining("OUTPUT_BYTE_LIMIT");
        assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
    }
    @Test void realChildRendersDirectOriginalAndVerifiesPersistedPixels() throws Exception {
        byte[] original=ReadingFixtures.quadrants("PNG"); String sha=ReadingIdentity.sha(original);
        var process=new ReadingImageProcess(); byte[] output=process.convert(original,ReadingFixtures.profile());
        assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        var disk=new ReadingFixtures.DiskStorage(directory);var storage=disk.storage();
        var stored=storage.create(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,"output","image/png",output,1_000_000);
        var decoded=ReadingImageInspector.decode(storage.read(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,stored,1_000_000),ReadingFixtures.profile());
        assertThat(decoded.image().getWidth()).isEqualTo(40);assertThat(decoded.image().getHeight()).isEqualTo(30);
        ReadingFixtures.assertQuadrants(decoded.image(),0);
        // A valid same-size blank PNG models deleting drawImage; metadata/SHA checks alone would accept it.
        var blank=new java.awt.image.BufferedImage(40,30,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var blankBytes=new ByteArrayOutputStream();javax.imageio.ImageIO.write(blank,"PNG",blankBytes);
        var blankStored=storage.create(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,"blank-mutant","image/png",blankBytes.toByteArray(),1_000_000);
        var blankDecoded=ReadingImageInspector.decode(storage.read(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,blankStored,1_000_000),ReadingFixtures.profile());
        assertThatThrownBy(() -> ReadingFixtures.assertQuadrants(blankDecoded.image(),0)).isInstanceOf(AssertionError.class);
        assertThat(ReadingIdentity.sha(original)).isEqualTo(sha);
        assertThat(storage.create(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,"output","image/png",output,1_000_000)).isEqualTo(stored);
        assertThatThrownBy(() -> storage.create(com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE,"output","image/png",original,1_000_000)).hasMessageContaining("IMMUTABLE_OBJECT_CONFLICT");
    }
    @Test void rejectsMalformedAlphaFramesMetadataByteAndPixelBombs() throws Exception {
        byte[] image=ReadingFixtures.image(80,60);
        assertThatThrownBy(() -> ReadingImageInspector.decode(Arrays.copyOf(image,image.length-4),ReadingFixtures.profile())).isInstanceOf(IOException.class);
        image[image.length-8]^=1;byte[] corrupt=image;
        assertThatThrownBy(() -> ReadingImageInspector.decode(corrupt,ReadingFixtures.profile())).isInstanceOf(IOException.class);
        var alpha=new java.awt.image.BufferedImage(80,60,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var stream=new ByteArrayOutputStream();javax.imageio.ImageIO.write(alpha,"PNG",stream);
        assertThatThrownBy(() -> ReadingImageInspector.decode(stream.toByteArray(),ReadingFixtures.profile())).hasMessageContaining("PNG_UNSUPPORTED_HEADER");
        assertThatThrownBy(() -> ReadingImageInspector.decode(ReadingFixtures.image(500,500),ReadingFixtures.profile())).hasMessageContaining("FULL_RASTER_PIXEL_LIMIT");
        assertThatThrownBy(() -> new ReadingImageProcess().convert(ReadingFixtures.image(20,20),ReadingFixtures.profile())).hasMessageContaining("SOURCE_SHORTAGE_NO_UPSCALE");
        byte[] trailing=Arrays.copyOf(ReadingFixtures.image(80,60),15000);
        assertThatThrownBy(() -> ReadingImageInspector.decode(trailing,ReadingFixtures.profile())).isInstanceOf(IOException.class);
    }
    @Test void timeoutAndConcurrencyObserveActualChildExit() throws Exception {
        var started=new CountDownLatch(1);
        var p=ReadingFixtures.profile(); var shortProfile=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),50,1000,1,1,true,"fixture");
        var process=new ReadingImageProcess() {
            @Override protected Process start(Path in,Path out,Path settings,Path log,TestProfile limits) throws IOException {
                var child=new ProcessBuilder("/bin/sleep","10").redirectOutput(log.toFile()).start();started.countDown();return child;
            }
        };
        try(var executor=Executors.newSingleThreadExecutor()) {
            var future=executor.submit(() -> assertThatThrownBy(() -> process.convert(ReadingFixtures.image(80,60),shortProfile)).hasMessageContaining("CHILD_TIMEOUT"));
            assertThat(started.await(3,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> new ReadingImageProcess().convert(ReadingFixtures.image(80,60),p)).hasMessageContaining("CONCURRENCY_EXHAUSTED");
            future.get(3,TimeUnit.SECONDS);
        }
        assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        assertThat(new ReadingImageProcess().convert(ReadingFixtures.image(80,60),p)).isNotEmpty();
    }
    @Test void storageRejectsWrongVersionMissingTruncatedAndTransactionIo() throws Exception {
        var disk=new ReadingFixtures.DiskStorage(directory);var storage=disk.storage(); var provider=com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE;
        byte[] original=ReadingFixtures.image(80,60);var object=storage.create(provider,"a","image/png",original,1_000_000);
        assertThatThrownBy(() -> storage.read(provider,new VersionedStorageObject("a","wrong","image/png",original.length),1_000_000)).hasMessageContaining("PINNED_VERSION_MISMATCH");
        Files.write(disk.file("a"),new byte[]{1});
        assertThatThrownBy(() -> storage.read(provider,object,1_000_000)).hasMessageContaining("SIZE_MISMATCH");
        assertThatThrownBy(() -> storage.observe(provider,"missing")).hasMessageContaining("MISSING");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { assertThatThrownBy(() -> storage.observe(provider,"a")).hasMessageContaining("INSIDE_TRANSACTION"); }
        finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
    }
    @Test void validCrcAnimationColorAndOversizedMetadataAreExplicitlyRejected() throws Exception {
        byte[] png=ReadingFixtures.image(80,60);
        for(String type:List.of("acTL","fcTL","iCCP","tRNS","eXIf","tEXt")) {
            var data=new byte[type.equals("tEXt")?70000:8];
            var chunk=new ByteArrayOutputStream();var bytes=new DataOutputStream(chunk);
            bytes.writeInt(data.length);byte[] name=type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);bytes.write(name);bytes.write(data);
            var crc=new java.util.zip.CRC32();crc.update(name);crc.update(data);bytes.writeInt((int)crc.getValue());
            var malformed=new ByteArrayOutputStream();malformed.write(png,0,33);malformed.write(chunk.toByteArray());malformed.write(png,33,png.length-33);
            assertThatThrownBy(() -> ReadingImageInspector.decode(malformed.toByteArray(),ReadingFixtures.profile())).hasMessageContaining("PNG_UNSUPPORTED_CHUNK:"+type);
        }
    }
    @Test void persistedWrongMimeShaDimensionsAndEncodedLimitCannotVerify() throws Exception {
        var disk=new ReadingFixtures.DiskStorage(directory);var storage=disk.storage();var provider=com.tellpal.v2.asset.domain.StorageProvider.FIREBASE_STORAGE;
        byte[] bytes=ReadingFixtures.image(40,30);var stored=storage.create(provider,"valid","image/png",bytes,1_000_000);
        var binding=ReadingFixtures.snapshot().bindings().getFirst();
        for(int kind=0;kind<3;kind++) {
            var object=kind==0?storage.create(provider,"wrong-mime","image/jpeg",bytes,1_000_000):stored;
            var output=new ReadingWork.Output(binding,"a".repeat(64),"b".repeat(64),"c".repeat(64),provider,object,kind==1?"0".repeat(64):ReadingIdentity.sha(bytes),kind==2?39:40,30);
            assertThatThrownBy(() -> com.tellpal.v2.asset.application.reading.ReadingImagePreparationService.verifyOutput(storage,output,ReadingFixtures.profile())).hasMessageContaining("PERSISTED_OUTPUT_VERIFICATION_FAILED");
        }
        assertThatThrownBy(() -> storage.read(provider,stored,1)).hasMessageContaining("BYTE_LIMIT");
    }
    @Test void resourceAdmissionAndActualRssOrDiskExhaustionFailWithObservedExit() throws Exception {
        var p=ReadingFixtures.profile();
        assertThatThrownBy(() -> new TestProfile(40,30,p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),1,p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),5000,30000,1,1,true,"fixture")).hasMessageContaining("Parent full-raster");
        for(String mode:List.of("memory","disk")) {
            var limits=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),mode.equals("memory")?80:p.processMiB(),p.diskBytes(),5000,30000,1,1,true,"fixture");
            var process=new ReadingImageProcess() {
                @Override protected Process start(Path in,Path out,Path settings,Path log,TestProfile profile) throws IOException {
                    return new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx256m","-cp",System.getProperty("java.class.path"),ResourceChild.class.getName(),mode,out.toString()).redirectOutput(log.toFile()).redirectErrorStream(true).start();
                }
            };
            assertThatThrownBy(() -> process.convert(ReadingFixtures.image(80,60),limits)).hasMessageContaining(mode.equals("memory")?"PROCESS_RSS_LIMIT":"TEMP_DISK_LIMIT");
            assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        }
    }
    public static class ResourceChild {
        public static void main(String[] args) throws Exception {
            if(args[0].equals("memory")) { byte[] memory=new byte[200*1024*1024];Arrays.fill(memory,(byte)1);Thread.sleep(10000);System.out.println(memory[0]); }
            else { Files.write(Path.of(args[1]),new byte[4*1024*1024]);Thread.sleep(10000); }
        }
    }
    @Test void exitingChildMayLoseItsRssStatusButLiveProbeErrorsStillFailClosed() throws Exception {
        for(boolean normalExit:List.of(true,false)) {
            var childRef=new java.util.concurrent.atomic.AtomicReference<Process>();
            var process=new ReadingImageProcess() {
                @Override protected Process start(Path in,Path out,Path settings,Path log,TestProfile profile) throws IOException {
                    Files.write(out,ReadingFixtures.image(40,30));
                    var child=new ProcessBuilder("/bin/sleep",normalExit?"0.15":"10").redirectOutput(log.toFile()).start();
                    childRef.set(child);return child;
                }
                @Override protected long probeRssKiB(long pid) throws IOException,InterruptedException {
                    if(normalExit) assertThat(childRef.get().waitFor(2,TimeUnit.SECONDS)).isTrue();
                    throw new NoSuchFileException("simulated-/proc/"+pid+"/status-race");
                }
            };
            if(normalExit) assertThat(process.convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
            else assertThatThrownBy(() -> process.convert(ReadingFixtures.image(80,60),ReadingFixtures.profile()))
                    .hasMessageContaining("RSS_PROBE_UNAVAILABLE:ROOT_PROCESS:","simulated-/proc/")
                    .hasCauseInstanceOf(NoSuchFileException.class);
            assertThat(childRef.get().isAlive()).isFalse();
        }
    }
    @Test void missingRssWithTransientAliveStateRequiresBoundedConfirmedExit() throws Exception {
        for(boolean boundedConfirmation:List.of(true,false)) assertTransientRssExit(boundedConfirmation);
    }
    private void assertTransientRssExit(boolean boundedConfirmation) throws Exception {
        var reference=new java.util.concurrent.atomic.AtomicReference<ExitTransitionProcess>();
        var process=new ReadingImageProcess() {
            @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile profile) throws IOException {
                Files.write(output,ReadingFixtures.image(40,30));
                var child=new ExitTransitionProcess(new ProcessBuilder("/bin/sleep","0.15").redirectOutput(log.toFile()).start(),boundedConfirmation);
                reference.set(child);return child;
            }
            @Override protected long probeRssKiB(long pid) throws IOException,InterruptedException {
                var child=reference.get();
                assertThat(child.delegate.waitFor(2,TimeUnit.SECONDS)).isTrue();
                child.staleAlive=true;
                throw new IOException("empty macOS ps RSS while Process completion notification is pending");
            }
        };
        assertThat(process.convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
        var child=reference.get();
        assertThat(child.transientAliveObserved).isTrue();
        assertThat(child.boundedExitConfirmations).isEqualTo(1);
        assertThat(child.isAlive()).isFalse();
    }
    private static final class ExitTransitionProcess extends Process {
        final Process delegate;
        final boolean boundedConfirmation;
        boolean staleAlive,transientAliveObserved;
        int boundedExitConfirmations;
        ExitTransitionProcess(Process delegate,boolean boundedConfirmation) { this.delegate=delegate;this.boundedConfirmation=boundedConfirmation; }
        @Override public OutputStream getOutputStream() { return delegate.getOutputStream(); }
        @Override public InputStream getInputStream() { return delegate.getInputStream(); }
        @Override public InputStream getErrorStream() { return delegate.getErrorStream(); }
        @Override public int waitFor() throws InterruptedException { staleAlive=false;return delegate.waitFor(); }
        @Override public boolean waitFor(long timeout,TimeUnit unit) throws InterruptedException {
            boolean exited=delegate.waitFor(timeout,unit);
            if(staleAlive) {
                assertThat(unit.toMillis(timeout)).isEqualTo(ReadingImageProcess.RSS_EXIT_CONFIRMATION_MILLIS);
                assertThat(exited).isTrue();boundedExitConfirmations++;staleAlive=false;
                return boundedConfirmation;
            }
            return exited;
        }
        @Override public int exitValue() { return delegate.exitValue(); }
        @Override public void destroy() { delegate.destroy(); }
        @Override public Process destroyForcibly() { delegate.destroyForcibly();return this; }
        @Override public long pid() { return delegate.pid(); }
        @Override public boolean isAlive() {
            if(staleAlive) { transientAliveObserved=true;return true; }
            return delegate.isAlive();
        }
    }
    @Test void interruptionRetainsCapacityUntilActualExitAndRestoresTheNextOperation() throws Exception {
        var started=new CountDownLatch(1);
        var workerThread=new java.util.concurrent.atomic.AtomicReference<Thread>();
        var operationDirectory=new java.util.concurrent.atomic.AtomicReference<Path>();
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        var process=new ReadingImageProcess() {
            @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile profile) throws IOException {
                operationDirectory.set(input.getParent());
                Process child=new ProcessBuilder("/bin/sleep","10").redirectOutput(log.toFile()).start();
                started.countDown();return child;
            }
        };
        try(var executor=Executors.newSingleThreadExecutor()) {
            var result=executor.submit(()-> {
                workerThread.set(Thread.currentThread());
                try { process.convert(ReadingFixtures.image(80,60),ReadingFixtures.profile());return null; }
                catch(IOException failure) { interrupted.set(Thread.currentThread().isInterrupted());return failure; }
            });
            assertThat(started.await(3,TimeUnit.SECONDS)).isTrue();
            workerThread.get().interrupt();
            assertThat(result.get(3,TimeUnit.SECONDS)).hasMessageContaining("CHILD_INTERRUPTED").hasCauseInstanceOf(InterruptedException.class);
        }
        assertThat(interrupted).isTrue();
        assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        assertThat(operationDirectory.get()).doesNotExist();
        assertThat(new ReadingImageProcess().convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
    }
    @Test void parentHeapAdmissionRejectsUnboundedReservationBeforeChildLaunch() throws Exception {
        var p=ReadingFixtures.profile();
        var excessive=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),Runtime.getRuntime().maxMemory(),
                p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,"fixture");
        var process=new ReadingImageProcess();
        assertThatThrownBy(() -> process.convert(ReadingFixtures.image(80,60),excessive)).hasMessageContaining("PARENT_HEAP_ADMISSION_EXCEEDED");
        assertThat(process.lastPid()).isZero();
        assertThat(ReadingProfiles.capture(Role.READING_PAGE,p).descriptor()).contains("parentAdmission","maxHeapDivisor","availableHeapDivisor","PARENT_ONLY");
    }
    @Test void canonicalFramingAndRoleSpecificProfileIdentity() throws Exception {
        assertThat(ReadingIdentity.hash("tellpal.profile.fixture.v1",Map.of("schema",1,"fixtureSetting","A"))).isEqualTo("109be3ad7fffcd6e1a5aadd356ff20ee43fb10451b8cc72b5afe891e513bc6b1");
        assertThat(ReadingIdentity.canonical(Map.of("text","A/\"\\\b\f\n\r\t\0\u001f\u007f"))).isEqualTo("{\"text\":\"A/\\\"\\\\\\b\\f\\n\\r\\t\\u0000\\u001f\\u007f\"}");
        assertThatThrownBy(() -> ReadingIdentity.canonical("ö")).hasMessageContaining("Non-ASCII");
        var profile=ReadingProfiles.capture(Role.READING_COVER,ReadingFixtures.profile());
        assertThat(ReadingProfiles.capture(Role.READING_DETAIL,ReadingFixtures.profile()).fingerprint()).isNotEqualTo(profile.fingerprint());
        try(var processClass=ReadingImageProcess.class.getResourceAsStream("/"+ReadingImageProcess.class.getName().replace('.','/')+".class")) {
            assertThat(processClass).isNotNull();
            assertThat(profile.descriptor()).contains("\""+ReadingImageProcess.class.getName()+"\":\""+ReadingIdentity.sha(processClass.readAllBytes())+"\"");
        }
        assertThat(profile.descriptor()).contains("MaxDirect".replace("MaxDirect","directMiB"),"rssProbe","compressionQuality","LOCAL_TEST_ONLY",
                "SDK_DEFAULT_NONIDEMPOTENT_HANDLER_FOR_BOTH_CLASSIFICATIONS", "SOCKET_INACTIVITY_WITH_BETWEEN_CALL_MONOTONIC_CHECKS",
                "DEFERRED_UNQUALIFIED_SLOW_TRICKLE", "FirebaseStorageAssetClient$1", "DefaultStorageRetryStrategy$InterceptorImpl");
    }
}
