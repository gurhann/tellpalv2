import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.reading.*;
import com.tellpal.v2.asset.infrastructure.media.reading.*;

/** Paketli parent ve gerçek child ile sentetik Linux/JRE koruma doğrulamasıdır. */
public final class PackagedPreservationProbe {
    public static void main(String[] args) throws Exception {
        if(args[0].equals("fault")) { faultChild(args);return; }
        Path directory=Path.of(args[0]);
        var fixture=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<60;y++) for(int x=0;x<80;x++) fixture.setRGB(x,y,y<30?(x<40?0xff0000:0x00ff00):(x<40?0x0000ff:0xffffff));
        var p=new TestProfile(100,100,1L<<20,1L<<20,10_000,32L<<20,96,16,384,8L<<20,20_000,60_000,1,1,true,
                "Synthetic packaged-runtime local growth admission only",FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP);
        var report=new ArrayList<Map<String,Object>>();
        for(String format:List.of("PNG","JPEG")) {
            var original=new ByteArrayOutputStream();ImageIO.write(fixture,format,original);byte[] input=original.toByteArray();
            var process=new ReadingImageProcess();byte[] result=process.convert(input,p);
            if(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)) throw new IllegalStateException("CHILD_STILL_ALIVE");
            Path output=directory.resolve(format.equals("JPEG")?"copy.jpg":"new.webp");Files.write(output,result,StandardOpenOption.CREATE_NEW);
            byte[] reread=Files.readAllBytes(output);ReadingPreservation.verify(input,reread,p);
            if(format.equals("JPEG") && !Arrays.equals(input,reread)) throw new IllegalStateException("JPEG_COPY_MISMATCH");
            report.add(Map.of("sourceFormat",format,"outputMime",ReadingPreservation.mime(reread),"bytes",reread.length,"sha256",ReadingIdentity.sha(reread),"fullRgbMetadataVerified",true,"childExited",true));
        }
        var legacy=new TestProfile(40,30,p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),p.heapMiB(),p.directMiB(),p.processMiB(),p.diskBytes(),p.sourceMillis(),p.revisionMillis(),1,1,true,"Legacy PNG fit only");
        var source=new ByteArrayOutputStream();ImageIO.write(fixture,"PNG",source);byte[] png=new ReadingImageProcess().convert(source.toByteArray(),legacy);
        var decoded=ReadingImageInspector.decode(png,legacy);if(decoded.image().getWidth()!=40 || decoded.image().getHeight()!=30) throw new IllegalStateException("LEGACY_FIT_MISMATCH");
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var result=Map.of("status","PASS","runtime",System.getProperty("java.runtime.version"),"os",System.getProperty("os.name"),"arch",System.getProperty("os.arch"),
                "profile",ReadingProfiles.capture(Role.READING_PAGE,p),"cases",report,"legacyPngFitVerified",true,"productionAdmission",false,"ownershipFaults",faults(directory,p));
        json.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("runtime.json").toFile(),result);
        System.out.println(json.writeValueAsString(result));
    }
    private static List<String> faultCommand(String mode, Path pids) {
        return List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx64m","-XX:+UseSerialGC",
                "-Dloader.path=/probe","-Dloader.main=PackagedPreservationProbe","-cp","/app/app.jar",
                "org.springframework.boot.loader.launch.PropertiesLauncher","fault",mode,pids.toString());
    }
    private static List<Map<String,Object>> faults(Path directory,TestProfile p) throws Exception {
        var results=new ArrayList<Map<String,Object>>();
        Process unrelated=new ProcessBuilder("/bin/sleep","120").start();
        try {
            for(String mode:List.of("late","abrupt","rss")) {
                Path pids=directory.resolve(mode+".pids");
                var observed=new HashMap<Long,Long>();
                var process=new ReadingImageProcess() {
                    @Override protected Process start(Path input,Path output,Path settings,Path log,TestProfile limits) throws java.io.IOException {
                        return startOwned(faultCommand(mode,pids),log);
                    }
                    @Override protected long probeRssKiB(long pid) throws java.io.IOException,InterruptedException {
                        long value=super.probeRssKiB(pid);observed.merge(pid,value,Math::max);return value;
                    }
                };
                var limits=new TestProfile(p.width(),p.height(),p.maxSourceBytes(),p.maxOutputBytes(),p.maxPixels(),p.parentMemoryBytes(),
                        64,16,mode.equals("rss")?192:384,p.diskBytes(),mode.equals("late")?4000:20_000,p.revisionMillis(),1,1,true,p.growthEvidence(),p.formatPolicy());
                String expected=mode.equals("late")?"CHILD_TIMEOUT":mode.equals("rss")?"PROCESS_RSS_LIMIT":"CHILD_REJECTED";
                try { process.convert(new byte[]{1},limits);throw new IllegalStateException("FAULT_ACCEPTED"); }
                catch(java.io.IOException failure) { if(!failure.getMessage().contains(expected)) throw failure; }
                var identities=Files.readAllLines(pids).stream().map(Long::parseLong).toList();
                if(identities.size()<2) throw new IllegalStateException("FAULT_NATIVE_NOT_STARTED");
                for(long pid:identities) if(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) throw new IllegalStateException("OWNED_PID_STILL_ALIVE:"+pid);
                if(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false) || !unrelated.isAlive()) throw new IllegalStateException("OWNERSHIP_EXIT_OR_ISOLATION_FAILED");
                if(mode.equals("rss") && observed.get(identities.getFirst())>=192*1024L) throw new IllegalStateException("WORKER_ALONE_EXCEEDS_RSS_LIMIT");
                results.add(Map.of("mode",mode,"expectedFailure",expected,"ownedPids",identities,"allExited",true,"unrelatedProcessAlive",true,"maxObservedRssKiB",observed));
            }
        } finally { unrelated.destroyForcibly();unrelated.waitFor(); }
        return results;
    }
    private static void faultChild(String[] args) throws Exception {
        String mode=args[1];Path pids=Path.of(args[2]);
        if(mode.equals("allocate")) { while(Files.readAllLines(pids).size()<2) Thread.sleep(5);byte[] memory=new byte[140*1024*1024];Arrays.fill(memory,(byte)1);Thread.sleep(30000);System.out.println(memory[0]);return; }
        Files.writeString(pids,ProcessHandle.current().pid()+"\n");
        do {
            Process descendant;
            if(mode.equals("rss")) {
                var command=new ArrayList<>(faultCommand("allocate",pids));command.set(1,"-Xmx256m");descendant=new ProcessBuilder(command).start();
            } else descendant=new ProcessBuilder("/bin/sleep","20").start();
            Files.writeString(pids,descendant.pid()+"\n",StandardOpenOption.APPEND);
            if(mode.equals("abrupt")) Runtime.getRuntime().halt(17);
            if(mode.equals("rss")) { descendant.waitFor();return; }
            Thread.sleep(120);
        } while(true);
    }

}
