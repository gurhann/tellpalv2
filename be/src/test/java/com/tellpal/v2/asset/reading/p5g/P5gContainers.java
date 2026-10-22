package com.tellpal.v2.asset.reading.p5g;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** MIME, geometri ve sınırlı container kabulü genel ImageIO keşfinden önce uygulanır. */
final class P5gContainers {
    private P5gContainers() { }

    static P5gMetadata inspect(byte[] bytes, String mime) throws IOException {
        P5gProfile.require(bytes.length <= P5gProfile.ENCODED, "FILE_BYTE_LIMIT");
        return switch (mime) {
            case "image/png" -> P5gPng.inspect(bytes);
            case "image/webp" -> P5gWebp.inspect(bytes);
            case "image/jpeg" -> jpeg(bytes);
            default -> throw new IOException("MIME_REFUSED");
        };
    }

    static void geometry(int width, int height) throws IOException {
        P5gProfile.require(width > 0 && height > 0 && (long) width * height <= P5gProfile.PIXELS, "PIXEL_LIMIT");
        P5gProfile.require(width == P5gProfile.WIDTH && height == P5gProfile.HEIGHT, "DIMENSIONS_REFUSED");
    }

    private static P5gMetadata jpeg(byte[] bytes) throws IOException {
        P5gProfile.require(bytes.length >= 4 && bytes[0] == (byte) 255 && bytes[1] == (byte) 216, "JPEG_SIGNATURE");
        int at = 2, metadata = 0; boolean sof = false, scan = false, end = false;
        byte[] tiff = null, xmp = null;
        while (at < bytes.length) {
            P5gProfile.require((bytes[at++] & 255) == 255 && at < bytes.length, "JPEG_MARKER");
            int marker = bytes[at++] & 255;
            if (marker == 217) { end = true; break; }
            P5gProfile.require(marker != 0 && marker != 255 && marker != 216 && at + 2 <= bytes.length, "JPEG_MARKER_REFUSED");
            int length = ushort(bytes, at);
            P5gProfile.require(length >= 2 && at + length <= bytes.length, "JPEG_SEGMENT");
            if (marker >= 224 || marker == 254) {
                metadata += length; P5gProfile.require(metadata <= P5gProfile.METADATA, "METADATA_LIMIT");
                byte[] body = Arrays.copyOfRange(bytes, at + 2, at + length);
                if (marker == 225) {
                    if (P5gMetadata.starts(body, new byte[]{'E','x','i','f',0,0})) {
                        P5gProfile.require(tiff == null, "JPEG_DUPLICATE_EXIF"); tiff = P5gMetadata.tiff(body);
                    } else {
                        byte[] prefix = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);
                        P5gProfile.require(xmp == null && P5gMetadata.starts(body, prefix), "JPEG_APP1_REFUSED");
                        xmp = Arrays.copyOfRange(body, prefix.length, body.length);
                    }
                } else P5gProfile.require(marker == 236 || marker == 238 || marker == 224, "JPEG_METADATA_REFUSED");
            } else if (marker == 192) {
                P5gProfile.require(!sof && length == 17 && bytes[at + 2] == 8 && bytes[at + 7] == 3, "JPEG_SOF");
                geometry(ushort(bytes, at + 5), ushort(bytes, at + 3)); sof = true;
            } else P5gProfile.require(marker == 219 || marker == 196 || marker == 221 || marker == 218, "JPEG_STRUCTURE_REFUSED");
            at += length;
            if (marker == 218) {
                P5gProfile.require(sof && !scan, "JPEG_SCAN"); scan = true;
                while (at < bytes.length) {
                    if ((bytes[at] & 255) != 255) { at++; continue; }
                    P5gProfile.require(at + 1 < bytes.length, "JPEG_ENTROPY_TRUNCATED");
                    int next = bytes[at + 1] & 255;
                    if (next == 0 || next >= 208 && next <= 215) { at += 2; continue; }
                    break;
                }
            }
        }
        P5gProfile.require(end && scan && at == bytes.length, "JPEG_END_OR_TRAILING");
        var result = new P5gMetadata(null, xmp, tiff); result.validate(); return result;
    }

    private static int ushort(byte[] bytes, int at) { return (bytes[at] & 255) << 8 | bytes[at + 1] & 255; }
}
