package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** RIFF sınırı, chunk sırası ve yalnız opak VP8L kabulü decode öncesinde sınanır. */
final class P5gWebp {
    private P5gWebp() { }

    static P5gMetadata inspect(byte[] bytes) throws IOException {
        P5gProfile.require(bytes.length >= 26 && bytes.length <= P5gProfile.ENCODED
                && text(bytes, 0).equals("RIFF") && text(bytes, 8).equals("WEBP")
                && unsigned(bytes, 4) + 8 == bytes.length, "WEBP_RIFF");
        Map<String, byte[]> chunks = new LinkedHashMap<>();
        int offset = 12, metadata = 0;
        while (offset < bytes.length) {
            P5gProfile.require(offset + 8 <= bytes.length, "WEBP_CHUNK_TRUNCATED");
            String type = text(bytes, offset); long length = unsigned(bytes, offset + 4);
            long end = offset + 8L + length;
            P5gProfile.require(end + length % 2 <= bytes.length, "WEBP_CHUNK_TRUNCATED");
            P5gProfile.require(length % 2 == 0 || bytes[(int) end] == 0, "WEBP_PADDING");
            P5gProfile.require(List.of("VP8X", "ICCP", "VP8L", "EXIF", "XMP ").contains(type)
                    && !chunks.containsKey(type), "WEBP_CHUNK_REFUSED");
            if (!type.equals("VP8L")) metadata = Math.addExact(metadata, (int) length);
            P5gProfile.require(metadata <= P5gProfile.METADATA, "METADATA_LIMIT");
            chunks.put(type, Arrays.copyOfRange(bytes, offset + 8, (int) end));
            offset = (int) (end + length % 2);
        }
        byte[] raster = chunks.get("VP8L");
        P5gProfile.require(raster != null && raster.length >= 5 && raster[0] == 0x2f, "WEBP_VP8L_REQUIRED");
        long bits = unsigned(raster, 1);
        P5gProfile.require(bits >>> 28 == 0 && (bits & 0x10000000L) == 0, "WEBP_VERSION_OR_ALPHA");
        P5gContainers.geometry((int) (bits & 0x3fff) + 1, (int) (bits >> 14 & 0x3fff) + 1);
        int flags = (chunks.containsKey("ICCP") ? 32 : 0) | (chunks.containsKey("EXIF") ? 8 : 0)
                | (chunks.containsKey("XMP ") ? 4 : 0);
        var expected = new ArrayList<String>();
        if (flags != 0) expected.add("VP8X");
        if ((flags & 32) != 0) expected.add("ICCP");
        expected.add("VP8L");
        if ((flags & 8) != 0) expected.add("EXIF");
        if ((flags & 4) != 0) expected.add("XMP ");
        P5gProfile.require(new ArrayList<>(chunks.keySet()).equals(expected), "WEBP_CHUNK_ORDER");
        if (flags != 0) {
            byte[] header = chunks.get("VP8X");
            P5gProfile.require(header.length == 10 && (header[0] & 255) == flags
                    && header[1] == 0 && header[2] == 0 && header[3] == 0, "WEBP_VP8X_FLAGS");
            P5gContainers.geometry(uint24(header, 4) + 1, uint24(header, 7) + 1);
        }
        var result = new P5gMetadata(chunks.get("ICCP"), chunks.get("XMP "), P5gMetadata.tiff(chunks.get("EXIF")));
        result.validate(); return result;
    }

    private static int uint24(byte[] b, int at) {
        return (b[at] & 255) | (b[at + 1] & 255) << 8 | (b[at + 2] & 255) << 16;
    }

    private static long unsigned(byte[] b, int at) {
        return Integer.toUnsignedLong(ByteBuffer.wrap(b, at, 4).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    private static String text(byte[] b, int at) {
        return new String(b, at, 4, StandardCharsets.US_ASCII);
    }
}
