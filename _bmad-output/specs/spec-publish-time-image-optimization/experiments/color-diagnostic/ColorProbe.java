import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ICC_ColorSpace;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Isolated pre-encode pixel observation using the current converter's reader flags. */
public final class ColorProbe {
    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void dump(BufferedImage image, Path destination) throws Exception {
        try (var output = new BufferedOutputStream(Files.newOutputStream(destination,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    output.write((rgb >>> 16) & 255);
                    output.write((rgb >>> 8) & 255);
                    output.write(rgb & 255);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5 || !"--source-root".equals(args[0]) || !"--output-root".equals(args[2])) {
            throw new IllegalArgumentException("Expected --source-root ROOT --output-root ROOT relative-images...");
        }
        Path sourceRoot = Path.of(args[1]).toRealPath();
        Path requestedOutput = Path.of(args[3]).toAbsolutePath().normalize();
        if (Files.exists(requestedOutput, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Output root must be fresh");
        }
        Path outputRoot = requestedOutput.getParent().toRealPath().resolve(requestedOutput.getFileName());
        if (outputRoot.startsWith(sourceRoot)) {
            throw new IllegalArgumentException("Output root must stay outside source root");
        }
        for (Path ancestor = outputRoot.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
            if (Files.exists(ancestor.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("Output root must stay outside repositories");
            }
        }
        Files.createDirectory(outputRoot);
        System.out.println("runtime|" + System.getProperty("java.version") + "|"
                + System.getProperty("java.vendor") + "|" + System.getProperty("java.vm.name"));
        for (int i = 4; i < args.length; i++) {
            String relative = args[i];
            Path path = sourceRoot.resolve(relative).toRealPath();
            if (Path.of(relative).isAbsolute() || !path.startsWith(sourceRoot)
                    || relative.contains("|") || relative.contains("\n")) {
                throw new IllegalArgumentException("Source must stay within its root");
            }
            BufferedImage source;
            String readerClass;
            String providerClass;
            String providerVendor;
            String providerVersion;
            try (var input = ImageIO.createImageInputStream(path.toFile())) {
                var reader = ImageIO.getImageReaders(input).next();
                try {
                    readerClass = reader.getClass().getName();
                    var provider = reader.getOriginatingProvider();
                    providerClass = provider.getClass().getName();
                    providerVendor = provider.getVendorName();
                    providerVersion = provider.getVersion();
                    reader.setInput(input, true, true);
                    source = reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
            String id = relative.replace('/', '-').replace('\\', '-');
            Path decodedPath = outputRoot.resolve(id + ".decoded.rgb");
            Path drawnPath = outputRoot.resolve(id + ".drawn.rgb");
            dump(source, decodedPath);
            BufferedImage drawn = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = drawn.createGraphics();
            try {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, drawn.getWidth(), drawn.getHeight());
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), null);
            } finally {
                graphics.dispose();
            }
            dump(drawn, drawnPath);
            var colorSpace = source.getColorModel().getColorSpace();
            String profileHash = colorSpace instanceof ICC_ColorSpace icc
                    ? sha256(icc.getProfile().getData()) : "none";
            System.out.println("decoded|" + relative + "|" + source.getWidth() + "|" + source.getHeight()
                    + "|" + source.getType() + "|" + colorSpace.getClass().getName() + "|" + colorSpace.isCS_sRGB()
                    + "|" + profileHash + "|" + readerClass + "|" + providerClass + "|" + providerVendor
                    + "|" + providerVersion + "|" + sha256(Files.readAllBytes(decodedPath))
                    + "|" + sha256(Files.readAllBytes(drawnPath)));
            source.flush();
            drawn.flush();
        }
    }
}
