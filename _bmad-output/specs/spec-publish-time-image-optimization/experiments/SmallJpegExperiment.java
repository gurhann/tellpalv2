package com.tellpal.v2.asset.infrastructure.media;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

/** Isolated file experiment using the unchanged production converter classes. */
public final class SmallJpegExperiment {
    private static final long MAX_PIXELS = 40_000_000;
    private record Candidate(String sample, String id, String role, int width, int height, int quality) {}
    private record Encoded(byte[] bytes, int width, int height, String mime, long transformNs) {}
    private record Call(Candidate candidate, int repetition) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Expected root, TSV, warmups, observations, seed");
        Path root = Path.of(args[0]);
        int warmups = Integer.parseInt(args[2]), observations = Integer.parseInt(args[3]);
        long seed = Long.parseLong(args[4]);
        List<Candidate> candidates = new ArrayList<>();
        Map<String, byte[]> sources = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Path.of(args[1]))) {
            String[] p = line.split("\t");
            if (p.length != 6) throw new IllegalArgumentException("Invalid candidate row");
            Candidate c = new Candidate(p[0], p[1], p[2], Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]));
            if (!c.sample().matches("[a-z0-9-]+") || !c.id().matches("[a-z0-9-]+")) throw new IllegalArgumentException("Unsafe ID");
            candidates.add(c);
            if (!sources.containsKey(c.sample())) sources.put(c.sample(), Files.readAllBytes(root.resolve("originals/" + c.sample() + ".jpg")));
        }
        if (sources.size() != 7 || candidates.size() != 63 || warmups != 2 || observations != 5)
            throw new IllegalArgumentException("Frozen experiment dimensions differ");
        recordRuntime(root, sources);
        Map<String, String> expected = new LinkedHashMap<>();
        for (Candidate c : candidates) for (int i = 0; i < warmups; i++) {
            Encoded image = encode(c, sources.get(c.sample()));
            assertRepeat(expected, c, image);
        }
        List<Call> calls = new ArrayList<>();
        for (int i = 0; i < observations; i++) for (Candidate c : candidates) calls.add(new Call(c, i));
        Collections.shuffle(calls, new Random(seed));
        StringBuilder times = new StringBuilder("sequence\tsample_id\tcandidate_id\trepetition\ttransform_ns\n");
        StringBuilder outputs = new StringBuilder("sample_id\tcandidate_id\trole\tquality\twidth\theight\tbytes\tsha256\tfile\tprocessor_profile\n");
        Map<String, Boolean> saved = new LinkedHashMap<>();
        int sequence = 0;
        for (Call call : calls) {
            Candidate c = call.candidate();
            byte[] bytes = sources.get(c.sample());
            Encoded image = encode(c, bytes);
            assertRepeat(expected, c, image);
            times.append(++sequence).append('\t').append(c.sample()).append('\t').append(c.id()).append('\t')
                    .append(call.repetition()).append('\t').append(image.transformNs()).append('\n');
            String key = key(c);
            if (saved.putIfAbsent(key, true) == null) {
                String file = "outputs/" + c.sample() + "--" + c.id() + ".jpg";
                Files.write(root.resolve(file), image.bytes(), StandardOpenOption.CREATE_NEW);
                String profile = c.role().equals("source_resolution_proxy")
                        ? new PageImageRenditionProcessor(c.width(), MAX_PIXELS, c.quality()).profileVersion()
                        : "listing-bounds-%dx%d-q%d-maxpixels-%d-white".formatted(c.width(), c.height(), c.quality(), MAX_PIXELS);
                outputs.append(c.sample()).append('\t').append(c.id()).append('\t').append(c.role()).append('\t')
                        .append(c.quality()).append('\t').append(image.width()).append('\t').append(image.height()).append('\t')
                        .append(image.bytes().length).append('\t').append(sha(image.bytes())).append('\t').append(file)
                        .append('\t').append(profile).append('\n');
            }
        }
        if (saved.size() != 63 || sequence != 315) throw new IllegalStateException("Incomplete experiment");
        Files.writeString(root.resolve("converter-observations.tsv"), times, StandardOpenOption.CREATE_NEW);
        Files.writeString(root.resolve("converter-output-manifest.tsv"), outputs, StandardOpenOption.CREATE_NEW);
        for (var entry : sources.entrySet()) if (!sha(entry.getValue()).equals(sha(Files.readAllBytes(root.resolve("originals/" + entry.getKey() + ".jpg")))))
            throw new IllegalStateException("Input bytes mutated");
        System.out.println("63 outputs; 126 warmups; 315 measured calls; all 441 output hashes deterministic");
    }

    private static Encoded encode(Candidate c, byte[] source) {
        if (c.role().equals("source_resolution_proxy")) {
            var processor = new PageImageRenditionProcessor(c.width(), MAX_PIXELS, c.quality());
            long start = System.nanoTime();
            var image = processor.process(source, "image/jpeg");
            long elapsed = System.nanoTime() - start;
            return new Encoded(image.bytes(), image.width(), image.height(), image.mimeType(), elapsed);
        }
        var processor = new ListingCoverRenditionProcessor(c.width(), c.height(), MAX_PIXELS, c.quality());
        long start = System.nanoTime();
        var image = processor.process(source, "image/jpeg");
        long elapsed = System.nanoTime() - start;
        return new Encoded(image.bytes(), image.width(), image.height(), image.mimeType(), elapsed);
    }

    private static void assertRepeat(Map<String, String> expected, Candidate c, Encoded image) throws Exception {
        if (!image.mime().equals("image/jpeg") || image.bytes().length == 0 || image.width() <= 0 || image.height() <= 0)
            throw new IllegalStateException("Invalid converter output");
        String fingerprint = sha(image.bytes()) + ":" + image.width() + ":" + image.height();
        String previous = expected.putIfAbsent(key(c), fingerprint);
        if (previous != null && !previous.equals(fingerprint)) throw new IllegalStateException("Non-deterministic repeat: " + key(c));
    }

    private static String key(Candidate c) { return c.sample() + "/" + c.id(); }
    private static String sha(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }

    private static void recordRuntime(Path root, Map<String, byte[]> sources) throws Exception {
        Properties p = new Properties();
        for (String key : List.of("java.version", "java.runtime.version", "java.vendor", "java.vm.name", "os.name", "os.version", "os.arch", "java.awt.headless"))
            p.setProperty(key, System.getProperty(key, "unavailable"));
        p.setProperty("imageio.use_cache", Boolean.toString(ImageIO.getUseCache()));
        p.setProperty("imageio.cache_directory", ImageIO.getCacheDirectory() == null ? "default_system_temporary_directory" : "explicit_private_directory");
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            p.setProperty("writer.class", writer.getClass().getName());
            p.setProperty("writer.spi", writer.getOriginatingProvider().getClass().getName());
            p.setProperty("writer.vendor", writer.getOriginatingProvider().getVendorName());
            p.setProperty("writer.version", writer.getOriginatingProvider().getVersion());
            p.setProperty("writer.compression_supported", Boolean.toString(writer.getDefaultWriteParam().canWriteCompressed()));
            p.setProperty("writer.metadata_policy", "null stream and image metadata; default JPEG sampling/progressive parameters");
            if (!writer.getDefaultWriteParam().canWriteCompressed()) throw new IllegalStateException("Explicit quality unsupported");
        } finally { writer.dispose(); }
        for (var source : sources.entrySet()) try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(source.getValue()))) {
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                String prefix = "reader." + source.getKey() + ".";
                p.setProperty(prefix + "class", reader.getClass().getName());
                p.setProperty(prefix + "spi", reader.getOriginatingProvider().getClass().getName());
                p.setProperty(prefix + "vendor", reader.getOriginatingProvider().getVendorName());
                p.setProperty(prefix + "version", reader.getOriginatingProvider().getVersion());
                p.setProperty(prefix + "format", reader.getFormatName());
            } finally { reader.dispose(); }
        }
        try (var stream = Files.newOutputStream(root.resolve("converter-runtime.properties"), StandardOpenOption.CREATE_NEW)) {
            p.store(stream, "Curated runtime properties; no credentials or private paths");
        }
    }
}
