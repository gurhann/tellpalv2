package com.tellpal.v2.asset.reading;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.*;
import com.tellpal.v2.asset.domain.StorageProvider;
import com.tellpal.v2.asset.infrastructure.storage.*;

public final class ReadingFixtures {
    private ReadingFixtures() { }
    public static TestProfile profile() {
        return new TestProfile(40,30,1_000_000,1_000_000,100_000,16_000_000,64,8,256,2_100_000,5000,30000,1,1,true,"Synthetic geometry/admission fixture only");
    }
    public static Snapshot snapshot() { return new Snapshot(9,"tr",List.of(new Binding(Role.READING_COVER,1,null,0,3),new Binding(Role.READING_DETAIL,1,null,0,3),new Binding(Role.READING_PAGE,2,11L,1,4)),List.of()); }
    public static Map<Role,TestProfile> profiles() { return Map.of(Role.READING_COVER,profile(),Role.READING_DETAIL,profile(),Role.READING_PAGE,profile()); }
    public static byte[] image(int width,int height) throws IOException {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        var random=new Random(71);
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) image.setRGB(x,y,random.nextInt(0xffffff));
        var output=new ByteArrayOutputStream(); ImageIO.write(image,"PNG",output); return output.toByteArray();
    }
    public static byte[] quadrants(String format) throws IOException {
        var image=new BufferedImage(80,60,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<60;y++) for(int x=0;x<80;x++) image.setRGB(x,y,
                y<30?(x<40?0xff0000:0x00ff00):(x<40?0x0000ff:0xffff00));
        var output=new ByteArrayOutputStream();
        if(!ImageIO.write(image,format,output)) throw new IOException("Fixture encoder unavailable");
        return output.toByteArray();
    }
    /** Independent original color constants, sampled well inside each quadrant to avoid resampling boundaries. */
    public static void assertQuadrants(BufferedImage image,int tolerance) {
        int[][] sentinels={{image.getWidth()/4,image.getHeight()/4,255,0,0},
                {image.getWidth()*3/4,image.getHeight()/4,0,255,0},
                {image.getWidth()/4,image.getHeight()*3/4,0,0,255},
                {image.getWidth()*3/4,image.getHeight()*3/4,255,255,0}};
        for(int[] sample:sentinels) {
            int actual=image.getRGB(sample[0],sample[1]);
            int[] channels={(actual>>>16)&255,(actual>>>8)&255,actual&255};
            for(int channel=0;channel<3;channel++) org.assertj.core.api.Assertions.assertThat(channels[channel])
                    .as("quadrant RGB channel at (%s,%s)",sample[0],sample[1]).isBetween(Math.max(0,sample[channel+2]-tolerance),Math.min(255,sample[channel+2]+tolerance));
        }
    }
    /** Real disk-backed create-only immutable objects; mutations are explicit negative-test fault injection. */
    public static class DiskStorage implements AssetStorageClient {
        public final Path directory;
        public final Map<String,VersionedStorageObject> objects=new java.util.concurrent.ConcurrentHashMap<>();
        public DiskStorage(Path directory) { this.directory=directory; }
        public AssetStorageClient withReadingDeadline(long millis) { return this; }
        public StorageProvider provider() { return StorageProvider.FIREBASE_STORAGE; }
        public StorageSignedDownloadUrl createSignedDownloadUrl(String path,Instant now) { throw new UnsupportedOperationException(); }
        public synchronized VersionedStorageObject createImmutable(String path,String mime,byte[] bytes) {
            return objects.computeIfAbsent(path,key -> {
                try { Files.createDirectories(file(key).getParent()); Files.write(file(key),bytes,StandardOpenOption.CREATE_NEW); }
                catch(IOException e) { throw new UncheckedIOException(e); }
                return new VersionedStorageObject(path,UUID.randomUUID().toString(),mime,bytes.length);
            });
        }
        public Optional<VersionedStorageObject> observeVersion(String path) { return Optional.ofNullable(objects.get(path)); }
        public InputStream openVersion(VersionedStorageObject object) {
            if(!object.equals(objects.get(object.path()))) throw new IllegalStateException("PINNED_VERSION_MISMATCH");
            try { return Files.newInputStream(file(object.path())); } catch(IOException e) { throw new UncheckedIOException(e); }
        }
        public Path file(String path) { return directory.resolve(path); }
        public ReadingObjectStorage storage() { return new ReadingObjectStorage(new AssetStorageClientRegistry(List.of(this))); }
    }
}
