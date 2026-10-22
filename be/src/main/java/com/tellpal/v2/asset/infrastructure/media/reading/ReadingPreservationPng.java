package com.tellpal.v2.asset.infrastructure.media.reading;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Inflater;

/** PNG kaynakları dönüşümsüz okunur; profil açma ve raster DEFLATE sınırları ayrıdır. */
final class ReadingPreservationPng {
    private ReadingPreservationPng() { }

    static ReadingPreservationMetadata inspect(byte[] bytes, TestProfile profile) throws IOException {
        byte[] signature = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        ReadingPreservation.require(ReadingPreservationMetadata.starts(bytes, signature), "PNG_SIGNATURE");
        int at = 8, budget = 0, channels = 0, width = 0, height = 0;
        boolean data = false, endedData = false, end = false;
        byte[] icc = null, xmp = null, tiff = null;
        var seen = new HashSet<String>(); var compressed = new ByteArrayOutputStream();
        while (at < bytes.length) {
            ReadingPreservation.require(bytes.length - at >= 12, "PNG_CHUNK_TRUNCATED");
            int length = ByteBuffer.wrap(bytes, at, 4).getInt();
            ReadingPreservation.require(length >= 0 && at + 12L + length <= bytes.length, "PNG_CHUNK_LENGTH");
            String kind = new String(bytes, at + 4, 4, StandardCharsets.US_ASCII);
            var crc = new CRC32(); crc.update(bytes, at + 4, length + 4);
            ReadingPreservation.require((int) crc.getValue() == ByteBuffer.wrap(bytes, at + 8 + length, 4).getInt(), "PNG_CRC");
            ReadingPreservation.require(!end && (at != 8 || kind.equals("IHDR")), "PNG_ORDER");
            if (!kind.equals("IDAT")) {
                ReadingPreservation.require(seen.add(kind), "PNG_DUPLICATE_CHUNK");
                budget += length; ReadingPreservation.require(budget <= ReadingPreservation.METADATA, "METADATA_LIMIT");
                if (data) endedData = true;
            }
            byte[] body = kind.equals("IDAT") ? null : Arrays.copyOfRange(bytes, at + 8, at + 8 + length);
            switch (kind) {
                case "IHDR" -> {
                    ReadingPreservation.require(length == 13 && body[8] == 8 && (body[9] == 2 || body[9] == 6)
                            && body[10] == 0 && body[11] == 0 && body[12] == 0, "PNG_HEADER");
                    width = ByteBuffer.wrap(body).getInt(); height = ByteBuffer.wrap(body, 4, 4).getInt();
                    ReadingPreservationContainers.geometry(width, height, profile);
                    channels = body[9] == 6 ? 4 : 3;
                }
                case "IDAT" -> {
                    ReadingPreservation.require(channels > 0 && !endedData, "PNG_IDAT_ORDER");
                    data = true; compressed.write(bytes, at + 8, length);
                }
                case "IEND" -> { ReadingPreservation.require(length == 0 && data && at + 12 == bytes.length, "PNG_END_OR_TRAILING"); end = true; }
                case "iCCP" -> { ReadingPreservation.require(!data, "PNG_METADATA_ORDER"); ReadingPreservation.require(!seen.contains("sRGB"), "PNG_COLOR_CONFLICT"); icc = icc(body); }
                case "eXIf" -> { ReadingPreservation.require(!data, "PNG_METADATA_ORDER"); tiff = ReadingPreservationMetadata.tiff(body); }
                case "iTXt" -> { ReadingPreservation.require(!data, "PNG_METADATA_ORDER"); xmp = xmp(body); }
                case "sRGB" -> ReadingPreservation.require(!data && icc == null && length == 1 && (body[0] & 255) <= 3, "PNG_SRGB");
                case "pHYs" -> {
                    ReadingPreservation.require(!data && length == 9 && (body[8] == 0 || body[8] == 1), "PNG_PHYS");
                    ReadingPreservation.require(ByteBuffer.wrap(body).getInt() == ByteBuffer.wrap(body, 4, 4).getInt(), "PNG_NONSQUARE_PIXEL_ASPECT_REFUSED");
                }
                default -> throw new IOException("PNG_CHUNK_REFUSED:" + kind);
            }
            at += 12 + length;
        }
        ReadingPreservation.require(end, "PNG_MISSING_END");
        inflate(compressed.toByteArray(), (long) height * (width * (long) channels + 1), false);
        var result = new ReadingPreservationMetadata(icc, xmp, tiff); result.validate(); return result;
    }

    private static byte[] icc(byte[] body) throws IOException {
        int zero = zero(body, 0);
        ReadingPreservation.require(zero >= 1 && zero <= 79 && zero + 2 < body.length && body[zero + 1] == 0, "PNG_ICCP");
        return inflate(Arrays.copyOfRange(body, zero + 2, body.length), ReadingPreservation.METADATA, true);
    }

    private static byte[] xmp(byte[] body) throws IOException {
        int keyword = zero(body, 0);
        ReadingPreservation.require(keyword > 0 && new String(body, 0, keyword, StandardCharsets.US_ASCII).equals("XML:com.adobe.xmp")
                && keyword + 5 <= body.length && body[keyword + 1] == 0 && body[keyword + 2] == 0, "PNG_XMP");
        int language = zero(body, keyword + 3), translated = zero(body, language + 1);
        return Arrays.copyOfRange(body, translated + 1, body.length);
    }

    private static int zero(byte[] bytes, int start) throws IOException {
        for (int i = start; i < bytes.length; i++) if (bytes[i] == 0) return i;
        throw new IOException("PNG_TEXT_TERMINATOR");
    }

    static byte[] inflate(byte[] compressed, long limit, boolean retain) throws IOException {
        var inflater = new Inflater(); var result = new ByteArrayOutputStream();
        try {
            inflater.setInput(compressed); long total = 0; byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                ReadingPreservation.require(n > 0, "PNG_DEFLATE_TRUNCATED");
                total += n; ReadingPreservation.require(total <= limit, "DECOMPRESSED_LIMIT");
                if (retain) result.write(buffer, 0, n);
            }
            ReadingPreservation.require(inflater.getRemaining() == 0 && (retain || total == limit), "PNG_DEFLATE_TRAILING_OR_SIZE");
            return result.toByteArray();
        } catch (java.util.zip.DataFormatException e) { throw new IOException("PNG_DEFLATE", e); }
        finally { inflater.end(); }
    }
}
