import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.domain.reading.ReadingProfiles;
import com.tellpal.v2.asset.infrastructure.media.reading.ReadingImageProcess;

/** Synthetic executable-JAR probe; numerical settings are test-only and grant no production admission. */
public class PackagedReadingProbe {
    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    public static void main(String[] args) throws Exception {
        var fixture = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 60; y++) for (int x = 0; x < 80; x++)
            fixture.setRGB(x, y, y < 30 ? (x < 40 ? 0xff0000 : 0x00ff00)
                    : (x < 40 ? 0x0000ff : 0xffffff));
        var source = new ByteArrayOutputStream();
        if (!ImageIO.write(fixture, "PNG", source)) throw new IllegalStateException("PNG writer unavailable");
        byte[] original = source.toByteArray();
        String sourceHash = sha(original);
        var profile = new TestProfile(40, 30, 1_048_576, 1_048_576, 10_000,
                33_554_432, 96, 16, 256, 4_194_304, 5_000, 30_000, 1, 1,
                true, "Synthetic packaged-runtime test, no production growth admission");
        System.out.println("SCOPE synthetic-packaged-reading-conversion-only; no-production-profile-admission");
        System.out.println("RUNTIME " + System.getProperty("java.runtime.version") + " "
                + System.getProperty("java.vendor") + " " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch"));
        System.out.println("PROFILE " + ReadingProfiles.capture(Role.READING_PAGE, profile).descriptor());
        var process = new ReadingImageProcess();
        byte[] encoded = process.convert(original, profile);
        if (ProcessHandle.of(process.lastPid()).map(ProcessHandle::isAlive).orElse(false))
            throw new IllegalStateException("Child still alive after conversion");
        Path stored = Path.of(args[0]).resolve("packaged-reading.png");
        Files.write(stored, encoded, java.nio.file.StandardOpenOption.CREATE_NEW);
        byte[] reread = Files.readAllBytes(stored);
        if (!sha(reread).equals(sha(encoded))) throw new IllegalStateException("Stored hash mismatch");
        try (var stream = ImageIO.createImageInputStream(stored.toFile())) {
            var reader = ImageIO.getImageReaders(stream).next();
            try {
                reader.setInput(stream, false, false);
                BufferedImage decoded = reader.read(0);
                if (!reader.getFormatName().equalsIgnoreCase("PNG") || reader.getWidth(0) != 40
                        || reader.getHeight(0) != 30 || decoded == null)
                    throw new IllegalStateException("Stored format/dimension/decode mismatch");
                int[][] sentinels = {{10, 7, 0xff0000}, {30, 7, 0x00ff00},
                        {10, 22, 0x0000ff}, {30, 22, 0xffffff}};
                for (int[] pixel : sentinels) if ((decoded.getRGB(pixel[0], pixel[1]) & 0xffffff) != pixel[2])
                    throw new IllegalStateException("Stored source-content sentinel mismatch");
            } finally { reader.dispose(); }
        }
        if (!sha(original).equals(sourceHash)) throw new IllegalStateException("Source bytes changed");
        System.out.println("VERIFIED packaged-child-exited=true source-content-sentinels=4 stored=PNG width=40 height=30 bytes="
                + reread.length + " sha256=" + sha(reread) + " sourceSha256=" + sourceHash);
        System.out.println("RESULT PASS actual executable-JAR child and independently reopened output");
    }
}
