package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ham metadata korunur; yalnız EXIF taşıma öneki TIFF karşılaştırmasından ayrılır. */
record P5gMetadata(byte[] icc, byte[] xmp, byte[] tiff) {
    static byte[] tiff(byte[] bytes) throws IOException {
        if (bytes == null) return null;
        byte[] prefix = {'E', 'x', 'i', 'f', 0, 0};
        byte[] data = starts(bytes, prefix) ? Arrays.copyOfRange(bytes, 6, bytes.length) : bytes;
        P5gProfile.require(data.length >= 8 && data.length <= P5gProfile.METADATA, "TIFF_SIZE");
        boolean little = data[0] == 'I' && data[1] == 'I';
        P5gProfile.require(little || data[0] == 'M' && data[1] == 'M', "TIFF_ENDIAN");
        var view = ByteBuffer.wrap(data).order(little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        P5gProfile.require(Short.toUnsignedInt(view.getShort(2)) == 42, "TIFF_MAGIC");
        inspectIfd(view, Integer.toUnsignedLong(view.getInt(4)), new HashSet<>());
        return data;
    }

    private static void inspectIfd(ByteBuffer data, long address, HashSet<Long> seen) throws IOException {
        if (address == 0) return;
        P5gProfile.require(seen.size() < 32 && seen.add(address) && address >= 8
                && address + 2 <= data.limit(), "TIFF_IFD_BOUNDS_OR_CYCLE");
        int offset = (int) address, count = Short.toUnsignedInt(data.getShort(offset));
        P5gProfile.require(address + 2L + 12L * count + 4 <= data.limit(), "TIFF_IFD_LENGTH");
        int[] lengths = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
        for (int i = 0; i < count; i++) {
            int at = offset + 2 + i * 12;
            int tag = Short.toUnsignedInt(data.getShort(at));
            P5gProfile.require(tag != 274, "ORIENTATION_REFUSED");
            P5gProfile.require(tag != 330, "TIFF_SUBIFD_REFUSED");
            int type = Short.toUnsignedInt(data.getShort(at + 2));
            long n = Integer.toUnsignedLong(data.getInt(at + 4));
            P5gProfile.require(type > 0 && type < lengths.length, "TIFF_TYPE");
            long size = n * lengths[type], value = Integer.toUnsignedLong(data.getInt(at + 8));
            P5gProfile.require(size <= P5gProfile.METADATA && (size <= 4 || value >= 8
                    && value + size <= data.limit()), "TIFF_VALUE_BOUNDS");
            if (tag == 34665 || tag == 34853 || tag == 40965) {
                P5gProfile.require(type == 4 && n == 1, "TIFF_POINTER");
                inspectIfd(data, value, seen);
            }
        }
        inspectIfd(data, Integer.toUnsignedLong(data.getInt(offset + 2 + count * 12)), seen);
    }

    void validate() throws IOException {
        long size = (icc == null ? 0 : icc.length) + (xmp == null ? 0 : xmp.length)
                + (tiff == null ? 0 : tiff.length);
        P5gProfile.require(size <= P5gProfile.METADATA, "METADATA_LIMIT");
        if (icc != null) P5gProfile.require(icc.length == 536 && P5gProfile.sha(icc).equals(P5gProfile.ICC_SHA), "ICC_REFUSED");
    }

    void same(P5gMetadata other) throws IOException {
        P5gProfile.require(Arrays.equals(icc, other.icc), "ICC_MISMATCH");
        P5gProfile.require(Arrays.equals(xmp, other.xmp), "XMP_MISMATCH");
        P5gProfile.require(Arrays.equals(tiff, other.tiff), "TIFF_MISMATCH");
    }

    Map<String, Object> summary() {
        var result = new LinkedHashMap<String, Object>();
        result.put("icc", summary(icc)); result.put("xmp", summary(xmp)); result.put("normalizedTiff", summary(tiff));
        return result;
    }

    private static Map<String, Object> summary(byte[] bytes) {
        return bytes == null ? Map.of("present", false, "bytes", 0)
                : Map.of("present", true, "bytes", bytes.length, "sha256", P5gProfile.sha(bytes));
    }

    static boolean starts(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }
}
