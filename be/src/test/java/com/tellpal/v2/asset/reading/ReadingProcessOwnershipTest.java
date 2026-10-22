package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess;

class ReadingProcessOwnershipTest {
    @TempDir Path directory;
    @Test void teardownConcurrentSpawnsCannotEscapeOwnedSessionOrReleaseCapacityEarly() throws Exception { run("late"); }
    @Test void abruptWorkerDeathCannotOrphanNativeBetweenSamples() throws Exception { run("abrupt"); }
    private void run(String mode) throws Exception {
        ReadingNativeTestTools.require();
        Path pids=directory.resolve(mode+".pids");
        var operationDirectory=new java.util.concurrent.atomic.AtomicReference<Path>();
        var started=new CountDownLatch(1);
        var process=new ReadingImageProcess() {
            @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile profile) throws IOException {
                operationDirectory.set(input.getParent());
                Process child=startOwned(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx64m","-cp",System.getProperty("java.class.path"),RaceChild.class.getName(),mode,pids.toString()),log);
                started.countDown();return child;
            }
        };
        var p=ReadingPreservationTest.profile();
        var limits=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),2000,10000,1,1,true,"fixture",p.formatPolicy());
        Process unrelated=new ProcessBuilder("/bin/sleep","15").start();
        try(var executor=Executors.newSingleThreadExecutor()) {
            var future=executor.submit(()->assertThatThrownBy(()->process.convert(ReadingFixtures.image(80,60),limits))
                    .hasMessageContaining(mode.equals("late")?"CHILD_TIMEOUT":"CHILD_REJECTED"));
            assertThat(started.await(3,TimeUnit.SECONDS)).isTrue();
            if(mode.equals("late")) assertThatThrownBy(()->new ReadingImageProcess().convert(ReadingFixtures.image(80,60),p)).hasMessageContaining("CONCURRENCY_EXHAUSTED");
            future.get(8,TimeUnit.SECONDS);
            assertThat(Files.readAllLines(pids)).isNotEmpty();
            for(String pid:Files.readAllLines(pids)) assertThat(ProcessHandle.of(Long.parseLong(pid)).map(ProcessHandle::isAlive).orElse(false)).as("PID %s",pid).isFalse();
            assertThat(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)).isFalse();
            assertThat(operationDirectory.get()).doesNotExist();
            assertThat(unrelated.isAlive()).isTrue();
            assertThat(new ReadingImageProcess().convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
        } finally { unrelated.destroyForcibly();unrelated.waitFor(); }
    }
    public static class RaceChild {
        public static void main(String[] args) throws Exception {
            Path pids=Path.of(args[1]);
            Files.writeString(pids,ProcessHandle.current().pid()+"\n");
            do {
                Process nativeChild=new ProcessBuilder("/bin/sleep","10").start();
                Files.writeString(pids,nativeChild.pid()+"\n",StandardOpenOption.APPEND);
                if(args[0].equals("abrupt")) Runtime.getRuntime().halt(17);
                Thread.sleep(30);
            } while(true);
        }
    }
}
