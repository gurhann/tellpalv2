package com.tellpal.v2.asset.reading;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess;

class ReadingImageLinuxRssTest {
    @TempDir Path directory;

    @Test void linuxParserAcceptsOnlyVerifiedTerminalStatesWithoutRss() throws Exception {
        for(String state:List.of("Z (zombie)","X (dead)")) {
            assertThat(Probe.parse(List.of("Name:\tfixture","State:\t"+state))).isZero();
        }
        for(String state:List.of("R (running)","S (sleeping)","T (stopped)","", "ZZ (invalid)")) {
            assertThatThrownBy(()->Probe.parse(List.of("State:\t"+state)))
                    .isInstanceOf(IOException.class).hasMessageContaining("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_MISSING_RSS");
        }
        assertThatThrownBy(()->Probe.parse(List.of("Name:\tunknown")))
                .hasMessageContaining("state=UNKNOWN");
    }
    @Test void linuxParserCountsValidRssAndRejectsMalformedOrOverflowingValues() throws Exception {
        assertThat(Probe.parse(List.of("State:\tR (running)","VmRSS:\t12345 kB"))).isEqualTo(12345);
        assertThat(Probe.parse(List.of("State:\tS (sleeping)","VmRSS:\t0 kB"))).isZero();
        for(String value:List.of("","invalid kB","-1 kB","12 MB","12","12 kB extra","9223372036854775808 kB")) {
            assertThatThrownBy(()->Probe.parse(List.of("State:\tS (sleeping)","VmRSS:\t"+value)))
                    .hasMessageContaining("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_RSS_PARSE_FAILED")
                    .hasCauseInstanceOf(NumberFormatException.class);
        }
    }
    @Test void missingAndUnreadableStatusHaveDistinctDiagnosticsAndOriginalCauses() throws Exception {
        assertThatThrownBy(()->Probe.read(directory.resolve("missing")))
                .hasMessageContaining("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_MISSING:")
                .hasCauseInstanceOf(NoSuchFileException.class);
        assertThatThrownBy(()->Probe.read(directory))
                .hasMessageContaining("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_READ_FAILED:")
                .hasCauseInstanceOf(IOException.class);
    }
    @Test @EnabledOnOs(OS.LINUX) void actualProcCountsLiveRssAndReportsMissingPid() throws Exception {
        assertThat(new Probe().sample(ProcessHandle.current().pid())).isPositive();
        assertThatThrownBy(()->new Probe().sample(Long.MAX_VALUE))
                .hasMessageContaining("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_MISSING:");
    }
    @Test @EnabledOnOs(OS.LINUX) void actualZombieHasNoRssButCapacityAndDirectoryRemainUntilReaped() throws Exception {
        try(var fixture=ControlledZombie.start(directory)) {
            assertThat(Files.readString(fixture.status())).contains("State:\tZ (zombie)").doesNotContain("VmRSS:");
            assertThat(new Probe().sample(fixture.pid)).isZero();
            var terminalSampled=new CountDownLatch(1);
            var operationDirectory=new AtomicReference<Path>();
            var process=new ReadingImageProcess() {
                @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile profile) throws IOException {
                    operationDirectory.set(input.getParent());Files.write(output,ReadingFixtures.image(40,30));
                    return fixture.notificationPending();
                }
                @Override protected long probeRssKiB(long pid) throws IOException,InterruptedException {
                    long actual=super.probeRssKiB(pid);
                    if(actual==0) terminalSampled.countDown();
                    return actual;
                }
            };
            try(var executor=Executors.newSingleThreadExecutor()) {
                var result=executor.submit(()->process.convert(ReadingFixtures.image(80,60),ReadingFixtures.profile()));
                try {
                    assertThat(terminalSampled.await(3,TimeUnit.SECONDS)).isTrue();
                    assertThat(result.isDone()).isFalse();assertThat(operationDirectory.get()).exists();
                    assertThatThrownBy(()->new ReadingImageProcess().convert(ReadingFixtures.image(80,60),ReadingFixtures.profile()))
                            .hasMessageContaining("CONCURRENCY_EXHAUSTED");
                } finally { fixture.release(); }
                assertThat(result.get(3,TimeUnit.SECONDS)).isEqualTo(ReadingFixtures.image(40,30));
            }
            assertThat(fixture.controller.exitValue()).isZero();
            assertThat(Files.readString(fixture.reaped)).isEqualTo(Long.toString(fixture.pid));
            assertThat(fixture.status()).doesNotExist();assertThat(operationDirectory.get()).doesNotExist();
            assertThat(new ReadingImageProcess().convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
        }
    }
    @Test @EnabledOnOs(OS.LINUX) void guardianMemberRssRaceAcceptsConfirmedExitAndKeepsRealTeardown() throws Exception {
        assertGuardianMemberRssFailure("exit");
    }
    @Test @EnabledOnOs(OS.LINUX) void livingGuardianMemberWithoutRssFailsAndObservesEntireTreeExit() throws Exception {
        assertGuardianMemberRssFailure("unavailable");
    }
    @Test @EnabledOnOs(OS.LINUX) void recoveredLiveGuardianMemberRssStillCountsTowardsTheRealLimit() throws Exception {
        assertGuardianMemberRssFailure("rss");
    }
    private void assertGuardianMemberRssFailure(String mode) throws Exception {
        ReadingNativeTestTools.require();
        Path nativePidFile=directory.resolve("native.pid"),workerPidFile=directory.resolve("worker.pid");
        var operationDirectory=new AtomicReference<Path>();
        var memberObservations=new java.util.concurrent.atomic.AtomicInteger();
        var nativeProbeInMonitor=new java.util.concurrent.atomic.AtomicBoolean();
        var sampledPid=new java.util.concurrent.atomic.AtomicLong();
        var recoveredRss=new java.util.concurrent.atomic.AtomicLong();
        var process=new ReadingImageProcess() {
            @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile profile) throws IOException {
                operationDirectory.set(input.getParent());
                return startOwned(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx64m",
                        "-XX:+UseSerialGC","-XX:ReservedCodeCacheSize=16m","-XX:MaxMetaspaceSize=32m",
                        "-cp",System.getProperty("java.class.path"),NativeExitChild.class.getName(),
                        nativePidFile.toString(),workerPidFile.toString(),output.toString(),mode),log);
            }
            @Override protected long probeRssKiB(long pid) throws IOException,InterruptedException {
                if(pid==lastPid()) nativeProbeInMonitor.set(false);
                if(Files.exists(nativePidFile)) {
                    String value=Files.readString(nativePidFile).trim();
                    if(!value.isEmpty() && pid==Long.parseLong(value)) {
                        sampledPid.set(pid);
                        int observation=memberObservations.getAndIncrement();
                        // Root probe her monitor başında sıfırlar; gerçek native RSS yalnız retry'da gelir.
                        boolean firstNativeProbe=mode.equals("rss") && !nativeProbeInMonitor.getAndSet(true);
                        if(mode.equals("unavailable") || observation==0 || firstNativeProbe) {
                            if(mode.equals("rss")) Files.writeString(Path.of(nativePidFile+".ready"),"ready");
                            throw new IOException("RSS_PROBE_UNAVAILABLE:LINUX_STATUS_MISSING_RSS:pid="+pid+":state=R");
                        }
                        if(mode.equals("exit")) ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
                        long actual=super.probeRssKiB(pid);
                        recoveredRss.accumulateAndGet(actual,Math::max);return actual;
                    }
                }
                return super.probeRssKiB(pid);
            }
        };
        if(mode.equals("exit")) assertThat(process.convert(ReadingFixtures.image(80,60),ReadingPreservationTest.profile()))
                .isEqualTo(ReadingFixtures.image(40,30));
        else if(mode.equals("rss")) {
            assertThatThrownBy(()->process.convert(ReadingFixtures.image(80,60),ReadingPreservationTest.profile()))
                    .hasMessageContaining("PROCESS_RSS_LIMIT");
            assertThat(recoveredRss.get()).isPositive();
        } else assertThatThrownBy(()->process.convert(ReadingFixtures.image(80,60),ReadingPreservationTest.profile()))
                .hasMessageContaining("RSS_PROBE_UNAVAILABLE:DESCENDANT:pid=","LINUX_STATUS_MISSING_RSS","state=R");
        assertThat(sampledPid.get()).isPositive();
        for(long pid:List.of(sampledPid.get(),Long.parseLong(Files.readString(workerPidFile)),process.lastPid())) {
            assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).as("PID %s",pid).isFalse();
        }
        assertThat(operationDirectory.get()).doesNotExist();
        assertThat(new ReadingImageProcess().convert(ReadingFixtures.image(80,60),ReadingFixtures.profile())).isNotEmpty();
    }
    public static final class NativeExitChild {
        public static void main(String[] args) throws Exception {
            Files.writeString(Path.of(args[1]),Long.toString(ProcessHandle.current().pid()));
            Process nativeChild=args[3].equals("rss")
                    ? new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx256m","-cp",
                            System.getProperty("java.class.path"),RecoveredRssChild.class.getName(),args[0]+".ready").start()
                    : new ProcessBuilder("/bin/sleep","10").start();
            Files.writeString(Path.of(args[0]),Long.toString(nativeChild.pid()));
            nativeChild.waitFor();
            Files.write(Path.of(args[2]),ReadingFixtures.image(40,30));
        }
    }
    public static final class RecoveredRssChild {
        public static void main(String[] args) throws Exception {
            while(!Files.exists(Path.of(args[0]))) Thread.sleep(1);
            byte[] allocation=new byte[200*1024*1024];Arrays.fill(allocation,(byte)1);
            Thread.sleep(10000);System.out.println(allocation[0]);
        }
    }
    private static final class Probe extends ReadingImageProcess {
        static long parse(List<String> lines) throws IOException { return parseLinuxRssKiB(42,lines); }
        static long read(Path path) throws IOException { return readLinuxRssKiB(42,path); }
        long sample(long pid) throws IOException,InterruptedException { return probeRssKiB(pid); }
    }
    private static final class ControlledZombie implements AutoCloseable {
        final Process controller;
        final Path reaped;
        long pid;
        private ControlledZombie(Process controller,Path reaped) { this.controller=controller;this.reaped=reaped; }
        static ControlledZombie start(Path directory) throws Exception {
            Path source=directory.resolve("zombie.c"),binary=directory.resolve("zombie"),pidFile=directory.resolve("zombie.pid");
            Path reaped=directory.resolve("reaped.pid"),log=directory.resolve("fixture.log");
            Files.writeString(source,SOURCE);
            Process compiler=new ProcessBuilder("cc","-std=c11","-D_POSIX_C_SOURCE=200809L","-Wall","-Wextra","-Werror",source.toString(),"-o",binary.toString())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                assertThat(compiler.waitFor(10,TimeUnit.SECONDS)).isTrue();
                assertThat(compiler.exitValue()).withFailMessage("%s",Files.readString(log)).isZero();
            } finally { if(compiler.isAlive()) compiler.destroyForcibly();compiler.waitFor(); }
            var fixture=new ControlledZombie(new ProcessBuilder(binary.toString(),pidFile.toString(),reaped.toString())
                    .redirectErrorStream(true).redirectOutput(log.toFile()).start(),reaped);
            try {
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while((!Files.exists(pidFile) || Files.readString(pidFile).isBlank()) && fixture.controller.isAlive() && System.nanoTime()<deadline) Thread.sleep(10);
                assertThat(pidFile).exists();fixture.pid=Long.parseLong(Files.readString(pidFile));return fixture;
            } catch(Exception | AssertionError failure) { fixture.close();throw failure; }
        }
        Path status() { return Path.of("/proc",Long.toString(pid),"status"); }
        void release() throws IOException { if(controller.isAlive()) { controller.getOutputStream().write('\n');controller.getOutputStream().flush(); } }
        Process notificationPending() {
            // Java çıkış bildirimi beklerken aynı OS PID'sinin terminal kanıtını kullanır.
            return new Process() {
                @Override public OutputStream getOutputStream() { return controller.getOutputStream(); }
                @Override public InputStream getInputStream() { return controller.getInputStream(); }
                @Override public InputStream getErrorStream() { return controller.getErrorStream(); }
                @Override public int waitFor() throws InterruptedException { return controller.waitFor(); }
                @Override public boolean waitFor(long timeout,TimeUnit unit) throws InterruptedException { return controller.waitFor(timeout,unit); }
                @Override public int exitValue() { return controller.exitValue(); }
                @Override public void destroy() { try { release(); } catch(IOException failure) { throw new UncheckedIOException(failure); } }
                @Override public Process destroyForcibly() { destroy();return this; }
                @Override public long pid() { return pid; }
                @Override public boolean isAlive() { return controller.isAlive(); }
            };
        }
        @Override public void close() throws Exception {
            release();
            assertThat(controller.waitFor(5,TimeUnit.SECONDS)).as("Fixture zombie çocuğunu reap etmeli").isTrue();
            if(pid!=0) assertThat(status()).doesNotExist();
        }
        private static final String SOURCE="""
                #include <errno.h>
                #include <stdio.h>
                #include <stdlib.h>
                #include <sys/types.h>
                #include <sys/wait.h>
                #include <unistd.h>
                int main(int argc,char **argv) {
                    if(argc!=3) return 2;
                    pid_t child=fork();
                    if(child<0) return 3;
                    if(child==0) _exit(0);
                    siginfo_t info={0};
                    if(waitid(P_PID,(id_t)child,&info,WEXITED|WNOWAIT)<0) { waitpid(child,NULL,0);return 4; }
                    FILE *pidfile=fopen(argv[1],"w");
                    if(!pidfile) { waitpid(child,NULL,0);return 5; }
                    fprintf(pidfile,"%ld",(long)child);fclose(pidfile);
                    char release;
                    while(read(STDIN_FILENO,&release,1)<0 && errno==EINTR) {}
                    int status;
                    if(waitpid(child,&status,0)!=child) return 6;
                    FILE *reaped=fopen(argv[2],"w");
                    if(!reaped) return 7;
                    fprintf(reaped,"%ld",(long)child);fclose(reaped);
                    return WIFEXITED(status)?WEXITSTATUS(status):8;
                }
                """;
    }
}
