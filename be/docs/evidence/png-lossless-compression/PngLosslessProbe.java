import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;
import com.tellpal.v2.asset.domain.reading.ReadingProfiles;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess;

/** Ayrı paketli motorun gerçek sınırlı süreç girişini yalnız yerel dosyalarla ölçer. */
public final class PngLosslessProbe {
    public static void main(String[] args) throws Exception {
        var mapper=new ObjectMapper();
        Path sources=Path.of(args[0]),outputs=Path.of(args[1]);
        Files.createDirectories(outputs);
        var plan=mapper.readTree(Path.of(args[2]).toFile());
        var profile=mapper.treeToValue(plan.get("preparation"),TestProfile.class);
        var profiles=new TreeMap<String,Object>();
        for(var role:Role.values()) {
            var captured=ReadingProfiles.capture(role,profile);
            profiles.put(role.name(),Map.of("fingerprint",captured.fingerprint(),"descriptor",mapper.readTree(captured.descriptor())));
        }
        var results=new ArrayList<Object>();
        for(int assetId:new int[]{9778,9816,9818}) {
            byte[] source=Files.readAllBytes(sources.resolve("canonical-review-"+assetId+".png"));
            var process=new ReadingImageProcess();
            long start=System.nanoTime();
            byte[] output=process.convert(source,profile);
            double elapsed=(System.nanoTime()-start)/1_000_000.0;
            if(ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false)) throw new IllegalStateException("CHILD_STILL_ALIVE");
            Path target=outputs.resolve(assetId+".png");Files.write(target,output);
            var keys=new TreeMap<String,String>();
            for(var role:assetId==9778?new Role[]{Role.READING_COVER,Role.READING_DETAIL}:new Role[]{Role.READING_PAGE}) {
                var captured=ReadingProfiles.capture(role,profile);
                keys.put(role.name(),ReadingIdentity.hash("tellpal.reading.rendition.v1",Map.of("schema",1,"role",role,
                        "sourceSha256",ReadingIdentity.sha(source),"profileSha256",captured.fingerprint())));
            }
            results.add(Map.of("assetId",assetId,"sourceSha256",ReadingIdentity.sha(source),"sourceBytes",source.length,
                    "outputSha256",ReadingIdentity.sha(output),"outputBytes",output.length,"processMillis",elapsed,
                    "childPid",process.lastPid(),"childExited",true,"renditionKeys",keys));
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(outputs.resolve("metadata.json").toFile(),
                Map.of("profile",profile,"profiles",profiles,"results",results,"classpath",System.getProperty("java.class.path"),
                        "processTimingMeaning","CHILD_STARTUP_HANDSHAKE_DECODE_RESIZE_ENCODE_MONITOR_OUTPUT_READ_AND_CLEANUP_NOT_PURE_ENCODE"));
    }
}
