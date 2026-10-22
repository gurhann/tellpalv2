package com.tellpal.v2.asset.infrastructure.media.reading;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** MIME, geometri ve sınırlı container kabulü genel ImageIO keşfinden önce uygulanır. */
final class ReadingPreservationContainers {
    private ReadingPreservationContainers() { }

    static ReadingPreservationMetadata inspect(byte[] bytes, String mime, TestProfile profile) throws IOException {
        ReadingPreservation.require(bytes.length > 0 && bytes.length <= Math.max(profile.maxSourceBytes(), profile.maxOutputBytes()), "FILE_BYTE_LIMIT");
        return switch (mime) {
            case "image/png" -> ReadingPreservationPng.inspect(bytes, profile);
            case "image/webp" -> ReadingPreservationWebp.inspect(bytes, profile);
            case "image/jpeg" -> jpeg(bytes, profile);
            default -> throw new IOException("MIME_REFUSED");
        };
    }

    static void geometry(int width, int height, TestProfile profile) throws IOException {
        ReadingPreservation.require(width <= profile.width() && height <= profile.height(), "PRESERVATION_GEOMETRY_LIMIT");
        ReadingPreservation.require(width > 0 && height > 0 && (long) width * height <= profile.maxPixels(), "PIXEL_LIMIT");
    }

    private static ReadingPreservationMetadata jpeg(byte[] bytes, TestProfile profile) throws IOException {
        ReadingPreservation.require(bytes.length >= 4 && bytes[0] == (byte) 255 && bytes[1] == (byte) 216, "JPEG_SIGNATURE");
        int at = 2, metadata = 0; boolean sof = false, scan = false, end = false;
        byte[] tiff = null, xmp = null;
        var iccParts = new java.util.TreeMap<Integer, byte[]>(); int iccCount = 0;
        while (at < bytes.length) {
            ReadingPreservation.require((bytes[at++] & 255) == 255 && at < bytes.length, "JPEG_MARKER");
            int marker = bytes[at++] & 255;
            if (marker == 217) { end = true; break; }
            ReadingPreservation.require(marker != 0 && marker != 255 && marker != 216 && at + 2 <= bytes.length, "JPEG_MARKER_REFUSED");
            int length = ushort(bytes, at);
            ReadingPreservation.require(length >= 2 && at + length <= bytes.length, "JPEG_SEGMENT");
            if (marker >= 224 || marker == 254) {
                metadata += length; ReadingPreservation.require(metadata <= ReadingPreservation.METADATA, "METADATA_LIMIT");
                byte[] body = Arrays.copyOfRange(bytes, at + 2, at + length);
                if (marker == 225) {
                    if (ReadingPreservationMetadata.starts(body, new byte[]{'E','x','i','f',0,0})) {
                        ReadingPreservation.require(tiff == null, "JPEG_DUPLICATE_EXIF"); tiff = ReadingPreservationMetadata.tiff(body);
                    } else {
                        byte[] prefix = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);
                        ReadingPreservation.require(xmp == null && ReadingPreservationMetadata.starts(body, prefix), "JPEG_APP1_REFUSED");
                        xmp = Arrays.copyOfRange(body, prefix.length, body.length);
                    }
                } else if (marker == 226) {
                    byte[] prefix = "ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII);
                    ReadingPreservation.require(body.length >= 14 && ReadingPreservationMetadata.starts(body, prefix), "JPEG_ICC_HEADER");
                    int sequence = body[12] & 255, total = body[13] & 255;
                    ReadingPreservation.require(total > 0 && sequence > 0 && sequence <= total && (iccCount == 0 || iccCount == total)
                            && !iccParts.containsKey(sequence), "JPEG_ICC_PARTS");
                    iccCount = total; iccParts.put(sequence, Arrays.copyOfRange(body, 14, body.length));
                } else if (marker == 224) {
                    ReadingPreservation.require(body.length >= 14 && ReadingPreservationMetadata.starts(body, new byte[]{'J','F','I','F',0})
                            && body.length == 14 + 3 * (body[12] & 255) * (body[13] & 255), "JPEG_JFIF_REFUSED");
                } else if (marker == 238) {
                    ReadingPreservation.require(body.length == 12 && ReadingPreservationMetadata.starts(body, "Adobe".getBytes(StandardCharsets.US_ASCII))
                            && (body[11] == 0 || body[11] == 1), "JPEG_ADOBE_COLOR_REFUSED");
                } else ReadingPreservation.require(marker == 236 && ReadingPreservationMetadata.starts(body, "Ducky".getBytes(StandardCharsets.US_ASCII)), "JPEG_METADATA_REFUSED");
            } else if (marker == 192) {
                ReadingPreservation.require(!sof && length == 17 && bytes[at + 2] == 8 && bytes[at + 7] == 3, "JPEG_SOF");
                geometry(ushort(bytes, at + 5), ushort(bytes, at + 3), profile); sof = true;
            } else ReadingPreservation.require(marker == 219 || marker == 196 || marker == 221 || marker == 218, "JPEG_STRUCTURE_REFUSED");
            at += length;
            if (marker == 218) {
                ReadingPreservation.require(sof && !scan, "JPEG_SCAN"); scan = true;
                while (at < bytes.length) {
                    if ((bytes[at] & 255) != 255) { at++; continue; }
                    ReadingPreservation.require(at + 1 < bytes.length, "JPEG_ENTROPY_TRUNCATED");
                    int next = bytes[at + 1] & 255;
                    if (next == 0 || next >= 208 && next <= 215) { at += 2; continue; }
                    break;
                }
            }
        }
        ReadingPreservation.require(end && scan && at == bytes.length, "JPEG_END_OR_TRAILING");
        byte[] icc = null;
        if (iccCount > 0) {
            ReadingPreservation.require(iccParts.size() == iccCount, "JPEG_ICC_PARTS");
            var combined = new java.io.ByteArrayOutputStream();
            for (int i = 1; i <= iccCount; i++) {
                ReadingPreservation.require(iccParts.containsKey(i), "JPEG_ICC_PARTS"); combined.writeBytes(iccParts.get(i));
            }
            icc = combined.toByteArray();
        }
        var result = new ReadingPreservationMetadata(icc, xmp, tiff); result.validate(); return result;
    }

    private static int ushort(byte[] bytes, int at) { return (bytes[at] & 255) << 8 | bytes[at + 1] & 255; }
}
