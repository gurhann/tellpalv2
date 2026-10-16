package com.tellpal.v2.asset.infrastructure.media.reading;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.domain.reading.ReadingDeadline;
import com.tellpal.v2.asset.application.reading.ReadingPreparationCapacity;

/** Holds one shared local reservation until actual child exit, including timeout/interrupt paths. */
public class ReadingImageProcess {
    public static final int RSS_EXIT_CONFIRMATION_MILLIS=20;
    private static final Semaphore CAPACITY=new Semaphore(1,true);
    private volatile long lastPid;
    private static final ThreadLocal<ReadingDeadline> REVISION_BUDGET=new ThreadLocal<>();
    public long lastPid() { return lastPid; }
    public byte[] convert(byte[] original,TestProfile profile,ReadingDeadline revision) throws IOException {
        revision.check();REVISION_BUDGET.set(revision);
        try { byte[] output=convert(original,profile);revision.check();return output; }
        finally { REVISION_BUDGET.remove(); }
    }
    public byte[] convert(byte[] original, TestProfile profile) throws IOException {
        ReadingPreparationCapacity.requireParentMemory(profile);
        ReadingDeadline revision=REVISION_BUDGET.get();
        long operationMillis=revision==null?profile.sourceMillis():revision.remainingMillis(profile.sourceMillis());
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(operationMillis);
        if (!CAPACITY.tryAcquire()) throw new IOException("LOCAL_CONCURRENCY_EXHAUSTED");
        Path directory=null; Process child=null;
        try {
            directory=Files.createTempDirectory("tellpal-reading-");
            Path input=directory.resolve("original"),output=directory.resolve("output.png"),settings=directory.resolve("profile.json"),log=directory.resolve("child.log");
            Files.write(input,original); Files.writeString(settings,new ObjectMapper().writeValueAsString(profile));
            child=start(input,output,settings,log,profile); lastPid=child.pid();
            while(!child.waitFor(20,TimeUnit.MILLISECONDS)) {
                if(System.nanoTime()>deadline) throw new IOException("CHILD_TIMEOUT");
                if(diskSize(directory)>profile.diskBytes()) throw new IOException("TEMP_DISK_LIMIT");
                if(residentMemoryKiB(child)>profile.processMiB()*1024L) throw new IOException("PROCESS_RSS_LIMIT");
            }
            if(System.nanoTime()>deadline) throw new IOException("CHILD_TIMEOUT");
            if(child.exitValue()!=0) throw new IOException("CHILD_REJECTED:"+boundedLog(log));
            if(diskSize(directory)>profile.diskBytes()) throw new IOException("TEMP_DISK_LIMIT");
            long size=Files.size(output);
            if(size<=0 || size>profile.maxOutputBytes()) throw new IOException("OUTPUT_BYTE_LIMIT");
            return Files.readAllBytes(output);
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("CHILD_INTERRUPTED",e); }
        finally {
            if(child!=null) terminateAndAwait(child);
            if(directory!=null) {
                try(var files=Files.walk(directory)) { for(Path file:files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file); }
                finally { CAPACITY.release(); }
            } else CAPACITY.release();
        }
    }
    protected Process start(Path input,Path output,Path settings,Path log,TestProfile p) throws IOException {
        var command=new ArrayList<>(List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),
                "-Xmx"+p.heapMiB()+"m","-XX:MaxDirectMemorySize="+p.directMiB()+"m","-XX:MaxMetaspaceSize=64m",
                "-XX:ReservedCodeCacheSize=32m","-Xss512k","-XX:+UseSerialGC","-XX:ActiveProcessorCount=1","-Djava.awt.headless=true",
                "-Dloader.main="+ReadingImageChild.class.getName(),"-cp",System.getProperty("java.class.path"),childMain(),input.toString(),output.toString(),settings.toString(),com.tellpal.v2.asset.domain.reading.ReadingProfiles.capture(com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.READING_PAGE,p).fingerprint()));
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    }
    private static String childMain() throws IOException {
        String classpath=System.getProperty("java.class.path");
        if(!classpath.contains(java.io.File.pathSeparator) && classpath.endsWith(".jar")) {
            try(var jar=new java.util.jar.JarFile(classpath)) {
                if(jar.getEntry("BOOT-INF/classes/"+ReadingImageChild.class.getName().replace('.', '/')+".class")!=null)
                    return "org.springframework.boot.loader.launch.PropertiesLauncher";
            }
        }
        return ReadingImageChild.class.getName();
    }
    private static long diskSize(Path directory) throws IOException {
        try(var files=Files.list(directory)) { long total=0; for(Path file:files.toList()) total=Math.addExact(total,Files.size(file)); return total; }
    }
    private static String boundedLog(Path log) throws IOException {
        try(var stream=Files.newInputStream(log)) { return new String(stream.readNBytes(2048),java.nio.charset.StandardCharsets.UTF_8); }
    }
    private long residentMemoryKiB(Process child) throws IOException,InterruptedException {
        try { return probeRssKiB(child.pid()); }
        catch(IOException | NumberFormatException failure) {
            // Process completion notification may trail the disappearing OS RSS entry.
            if(!child.isAlive() || child.waitFor(RSS_EXIT_CONFIRMATION_MILLIS,TimeUnit.MILLISECONDS)) return 0;
            throw new IOException("RSS_PROBE_UNAVAILABLE",failure);
        }
    }
    protected long probeRssKiB(long pid) throws IOException, InterruptedException {
        Path status=Path.of("/proc",Long.toString(pid),"status");
        if(System.getProperty("os.name").equals("Linux")) {
            if(!Files.exists(status)) throw new IOException("RSS_PROBE_UNAVAILABLE");
            for(String line:Files.readAllLines(status)) if(line.startsWith("VmRSS:")) return Long.parseLong(line.substring(6).trim().split("\\s+")[0]);
            throw new IOException("RSS_PROBE_UNAVAILABLE");
        }
        Process probe=new ProcessBuilder("ps","-o","rss=","-p",Long.toString(pid)).start();
        try {
            if(!probe.waitFor(100,TimeUnit.MILLISECONDS)) throw new IOException("RSS_PROBE_TIMEOUT");
            String value=new String(probe.getInputStream().readNBytes(64),java.nio.charset.StandardCharsets.US_ASCII).trim();
            if(value.isEmpty()) throw new IOException("RSS_PROBE_UNAVAILABLE");
            return Long.parseLong(value);
        } catch(NumberFormatException e) { throw new IOException("RSS_PROBE_UNAVAILABLE",e); }
        finally { terminateAndAwait(probe); }
    }
    private static void terminateAndAwait(Process process) {
        boolean interrupted=Thread.interrupted();
        if(process.isAlive()) process.destroyForcibly();
        for(;;) {
            try { process.waitFor(); break; } catch(InterruptedException e) { interrupted=true; process.destroyForcibly(); }
        }
        if(interrupted) Thread.currentThread().interrupt();
    }
}
