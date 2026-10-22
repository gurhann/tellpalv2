package com.tellpal.v2.asset.infrastructure.media.reading;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.FormatPolicy;
import com.tellpal.v2.asset.domain.reading.ReadingIdentity;

/** Koruma politikasının container, ham RGB ve metadata sözleşmesini tek noktada uygular. */
public final class ReadingPreservation {
    static final int METADATA = 65_536;
    static final String ICC_SHA = "20789fdbea9835251a4f0796c8bf45cbd964896044886540da21ffc7457af0ab";
    static final String SPI = "com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi";
    private ReadingPreservation() { }

    public static boolean enabled(TestProfile profile) {
        return profile.formatPolicy() == FormatPolicy.PRESERVE_JPEG_OR_LOSSLESS_WEBP;
    }

    /** Yalnız gerçek imzadan MIME çıkarır; storage MIME karşılaştırmasını çağıran yapar. */
    public static String mime(byte[] bytes) throws IOException {
        if (bytes.length >= 8 && bytes[0] == (byte) 137) return "image/png";
        if (bytes.length >= 4 && bytes[0] == (byte) 255 && bytes[1] == (byte) 216) return "image/jpeg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I') return "image/webp";
        throw new IOException("PNG_JPEG_OR_WEBP_REQUIRED");
    }

    static ReadingImageInspector.Decoded decode(byte[] bytes, TestProfile profile) throws IOException {
        try {
            String mime = mime(bytes);
            var raw = ReadingPreservationRawDecoder.decode(bytes, mime, profile);
            return new ReadingImageInspector.Decoded(image(raw), mime);
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("PRESERVATION_DECODE_REJECTED", e); }
    }

    static BufferedImage image(ReadingPreservationRawDecoder.Decoded decoded) {
        var image = new BufferedImage(decoded.width(), decoded.height(), BufferedImage.TYPE_3BYTE_BGR);
        int[] row = new int[Math.multiplyExact(decoded.width(), 3)]; int at = 0;
        for (int y = 0; y < decoded.height(); y++) {
            for (int i = 0; i < row.length; i++) row[i] = decoded.rgb()[at++] & 255;
            image.getRaster().setPixels(0, y, decoded.width(), 1, row);
        }
        return image;
    }

    /** Kaynak ile bağımsız okunan çıktı tam RGB ve ICC/XMP/normalize TIFF eşliği taşımalıdır. */
    public static void verify(byte[] source, byte[] output, TestProfile profile) throws IOException {
        if (!enabled(profile)) return;
        try {
            String inputMime = mime(source), outputMime = mime(output);
            require(!inputMime.equals("image/webp"), "SOURCE_PNG_OR_JPEG_REQUIRED");
            require(outputMime.equals(outputMime(inputMime, profile)), "OUTPUT_POLICY_MIME_MISMATCH");
            var original = ReadingPreservationRawDecoder.decode(source, inputMime, profile);
            var candidate = ReadingPreservationRawDecoder.decode(output, outputMime, profile);
            ReadingPreservationRawDecoder.same(original, candidate);
            if (inputMime.equals("image/jpeg")) require(Arrays.equals(source, output), "JPEG_BYTE_COPY_REQUIRED");
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException("PRESERVATION_PROOF_REJECTED", e); }
    }

    public static String outputMime(String sourceMime, TestProfile profile) {
        return enabled(profile) ? (sourceMime.equals("image/jpeg") ? "image/jpeg" : "image/webp") : "image/png";
    }

    static String sha(byte[] bytes) { return ReadingIdentity.sha(bytes); }
    static void require(boolean condition, String diagnostic) throws IOException {
        if (!condition) throw new IOException(diagnostic);
    }
}
