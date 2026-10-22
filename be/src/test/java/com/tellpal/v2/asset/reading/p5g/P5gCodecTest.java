package com.tellpal.v2.asset.reading.p5g;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "p5g.local.enabled", matches = "true")
class P5gCodecTest {
    static byte[] png, webp;
    static P5gRawDecoder.Decoded source, candidate;

    @BeforeAll static void fixture() throws Exception {
        Path report = Path.of(System.getProperty("p5g.report"));
        var rows = P5gCorpus.load(report);
        var row = rows.stream().filter(item -> item.sourceMime().equals("image/png")).findFirst().orElseThrow();
        png = P5gProfile.read(row.source(), P5gProfile.ENCODED);
        webp = P5gProfile.read(row.candidate(), P5gProfile.ENCODED);
        source = P5gRawDecoder.decode(png, "image/png"); candidate = P5gRawDecoder.decode(webp, "image/webp");
    }

    @Test void p3SentinelPreservesAllRawSamplesAndDefaultReadChangesThem() throws Exception {
        assertDoesNotThrow(() -> P5gRawDecoder.same(source, candidate));
        assertEquals("8f9ce6de504a8b785b5fdca111b2cc0fcbadc47b817366d5258c06f08b63d918", P5gProfile.sha(candidate.rgb()));
        var defaultImage = ImageIO.read(new ByteArrayInputStream(webp));
        try { assertFalse(Arrays.equals(candidate.rgb(), P5gRawDecoder.samples(defaultImage))); }
        finally { defaultImage.flush(); }
    }

    @Test void mimeAndTruncationAndTrailingAreRefused() {
        assertThrows(IOException.class, () -> P5gContainers.inspect(webp, "image/png"));
        assertThrows(IOException.class, () -> P5gContainers.inspect(png, "image/jpeg"));
        assertThrows(IOException.class, () -> P5gContainers.inspect(webp, "application/octet-stream"));
        assertThrows(IOException.class, () -> P5gWebp.inspect(Arrays.copyOf(webp, webp.length - 1)));
        assertThrows(IOException.class, () -> P5gWebp.inspect(Arrays.copyOf(webp, webp.length + 1)));
        assertThrows(IOException.class, () -> P5gPng.inspect(Arrays.copyOf(png, png.length + 1)));
    }

    @Test void lossyAnimationAlphaVersionAndCorruptHeaderAreRefusedBeforeDecode() {
        int raster = chunk(webp, "VP8L"), extended = chunk(webp, "VP8X");
        for (int mode = 0; mode < 5; mode++) {
            byte[] changed = webp.clone();
            if (mode == 0) changed[raster + 3] = ' ';
            if (mode == 1) changed[extended + 8] |= 2;
            if (mode == 2) changed[raster + 12] |= 16;
            if (mode == 3) changed[raster + 12] |= 32;
            if (mode == 4) changed[raster + 8] = 0;
            assertThrows(IOException.class, () -> P5gWebp.inspect(changed));
        }
    }

    @Test void pixelEncodedAndMetadataLimitsAreRefused() throws Exception {
        assertThrows(IOException.class, () -> P5gContainers.inspect(new byte[(int) P5gProfile.ENCODED + 1], "image/webp"));
        byte[] huge = webp.clone(); int raster = chunk(huge, "VP8L");
        ByteBuffer.wrap(huge, raster + 9, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(16_383 | 16_383 << 14);
        assertEquals("PIXEL_LIMIT", assertThrows(IOException.class, () -> P5gWebp.inspect(huge)).getMessage());
        byte[] metadata = new byte[12 + 8 + 65_537 + 1];
        System.arraycopy("RIFF".getBytes(), 0, metadata, 0, 4);
        ByteBuffer.wrap(metadata, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(metadata.length - 8);
        System.arraycopy("WEBPICCP".getBytes(), 0, metadata, 8, 8);
        ByteBuffer.wrap(metadata, 16, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(65_537);
        assertEquals("METADATA_LIMIT", assertThrows(IOException.class, () -> P5gWebp.inspect(metadata)).getMessage());
        var compressed = new java.io.ByteArrayOutputStream();
        try (var deflate = new java.util.zip.DeflaterOutputStream(compressed)) { deflate.write(new byte[65_537]); }
        assertEquals("DECOMPRESSED_LIMIT", assertThrows(IOException.class,
                () -> P5gPng.inflate(compressed.toByteArray(), 65_536, true)).getMessage());
    }

    @Test void rgbIccXmpAndTiffMismatchesAreDistinctFailures() throws Exception {
        byte[] rgb = candidate.rgb().clone(); rgb[rgb.length - 1] ^= 1;
        assertEquals("RGB_MISMATCH", assertThrows(IOException.class, () -> P5gRawDecoder.same(source,
                new P5gRawDecoder.Decoded(rgb, candidate.metadata(), candidate.reader()))).getMessage());
        var metadata = candidate.metadata();
        byte[] icc = metadata.icc().clone(); icc[icc.length - 1] ^= 1;
        assertEquals("ICC_REFUSED", assertThrows(IOException.class,
                () -> new P5gMetadata(icc, metadata.xmp(), metadata.tiff()).validate()).getMessage());
        for (String key : List.of("ICC", "XMP", "TIFF")) {
            var changed = new P5gMetadata(key.equals("ICC") ? null : metadata.icc(),
                    key.equals("XMP") ? null : metadata.xmp(), key.equals("TIFF") ? null : metadata.tiff());
            assertEquals(key + "_MISMATCH", assertThrows(IOException.class, () -> metadata.same(changed)).getMessage());
        }
    }

    @Test void exifPrefixOnlyIsNormalizedAndEveryOrientationIsRefused() throws Exception {
        byte[] tiff = source.metadata().tiff(); byte[] wrapped = new byte[tiff.length + 6];
        System.arraycopy(new byte[]{'E','x','i','f',0,0}, 0, wrapped, 0, 6);
        System.arraycopy(tiff, 0, wrapped, 6, tiff.length);
        assertArrayEquals(tiff, P5gMetadata.tiff(wrapped));
        byte[] oriented = tiff.clone(); oriented[10] = 1; oriented[11] = 18;
        assertEquals("ORIENTATION_REFUSED", assertThrows(IOException.class, () -> P5gMetadata.tiff(oriented)).getMessage());
        byte[] corrupt = tiff.clone(); Arrays.fill(corrupt, 4, 8, (byte) 255);
        assertThrows(IOException.class, () -> P5gMetadata.tiff(corrupt));
        byte[] subIfd = tiff.clone(); subIfd[10] = 1; subIfd[11] = 74;
        assertEquals("TIFF_SUBIFD_REFUSED", assertThrows(IOException.class, () -> P5gMetadata.tiff(subIfd)).getMessage());
    }

    @Test void trailingBytesInsideVp8lAreRefusedDespiteValidRiffAndChunkLengths() throws Exception {
        int raster = chunk(webp, "VP8L");
        int length = ByteBuffer.wrap(webp, raster + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
        for (int extra : new int[]{1, 2, 8}) {
            int padding = (length + extra) % 2;
            int oldEnd = raster + 8 + length + length % 2;
            byte[] changed = new byte[webp.length + extra + padding - length % 2];
            System.arraycopy(webp, 0, changed, 0, raster + 8 + length);
            Arrays.fill(changed, raster + 8 + length, raster + 8 + length + extra, (byte) 42);
            int newEnd = raster + 8 + length + extra + padding;
            System.arraycopy(webp, oldEnd, changed, newEnd, webp.length - oldEnd);
            ByteBuffer.wrap(changed, raster + 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(length + extra);
            ByteBuffer.wrap(changed, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(changed.length - 8);
            assertDoesNotThrow(() -> P5gWebp.inspect(changed));
            assertEquals("WEBP_PAYLOAD_TRAILING_OR_TRUNCATED", assertThrows(IOException.class,
                    () -> P5gRawDecoder.decode(changed, "image/webp")).getMessage());
        }
    }

    @Test void actualTransparentRasterIsRefused() {
        var image = new BufferedImage(P5gProfile.WIDTH, P5gProfile.HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        try { assertEquals("TRANSPARENT_PIXEL", assertThrows(IOException.class, () -> P5gRawDecoder.samples(image)).getMessage()); }
        finally { image.flush(); }
    }

    @Test void currentP2StillRejectsP3PngAndWebp() {
        assertEquals("REJECTED", P5gExistingAdmission.inspect(png).get("status"));
        assertEquals("REJECTED", P5gExistingAdmission.inspect(webp).get("status"));
    }

    static int chunk(byte[] bytes, String name) {
        for (int at = 12; at < bytes.length;) {
            if (new String(bytes, at, 4, java.nio.charset.StandardCharsets.US_ASCII).equals(name)) return at;
            int n = ByteBuffer.wrap(bytes, at + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt(); at += 8 + n + n % 2;
        }
        throw new IllegalArgumentException(name);
    }
}
