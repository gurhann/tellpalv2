package com.tellpal.v2.asset.reading.p5g;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Profil dönüşümü yerine reader'ın raw destination'ını ve mantıksal RGB bantlarını kullanır. */
final class P5gRawDecoder {
    record Decoded(byte[] rgb, P5gMetadata metadata, String reader) { }

    private P5gRawDecoder() { }

    static Decoded decode(byte[] bytes, String mime) throws Exception {
        P5gProfile.enabled();
        P5gMetadata metadata = P5gContainers.inspect(bytes, mime);
        var warnings = new ArrayList<String>();
        ImageReader reader = reader(mime);
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.addIIOReadWarningListener((ignored, warning) -> warnings.add(warning));
            reader.setInput(stream, false, false);
            P5gContainers.geometry(reader.getWidth(0), reader.getHeight(0));
            P5gProfile.require(reader.getNumImages(true) == 1, "SINGLE_FRAME_REQUIRED");
            var raw = reader.getRawImageType(0);
            P5gProfile.require(raw != null && raw.getColorModel().getNumColorComponents() == 3
                    && raw.getColorModel().getColorSpace().getType() == ColorSpace.TYPE_RGB, "RAW_RGB_REQUIRED");
            for (int size : raw.getSampleModel().getSampleSize()) P5gProfile.require(size == 8, "RGB8_REQUIRED");
            P5gProfile.require(raw.getSampleModel().getDataType() == DataBuffer.TYPE_BYTE, "BYTE_RASTER_REQUIRED");
            var param = reader.getDefaultReadParam(); param.setDestinationType(raw);
            BufferedImage image = reader.read(0, param);
            try {
                P5gProfile.require(warnings.isEmpty(), "DECODE_WARNING:" + String.join(";", warnings));
                if (mime.equals("image/webp")) verifyWebpConsumption(bytes, image);
                return new Decoded(samples(image), metadata, reader.getClass().getName());
            } finally { image.flush(); }
        } finally { reader.dispose(); }
    }

    static byte[] samples(BufferedImage image) throws IOException {
        P5gContainers.geometry(image.getWidth(), image.getHeight());
        var raster = image.getRaster(); var model = image.getColorModel();
        P5gProfile.require(model.getNumColorComponents() == 3 && model.getColorSpace().getType() == ColorSpace.TYPE_RGB
                && !model.isAlphaPremultiplied() && raster.getNumBands() == (model.hasAlpha() ? 4 : 3), "RAW_BANDS");
        for (int size : raster.getSampleModel().getSampleSize()) P5gProfile.require(size == 8, "RGB8_REQUIRED");
        byte[] rgb = new byte[P5gProfile.WIDTH * P5gProfile.HEIGHT * 3]; int offset = 0;
        int[] row = new int[P5gProfile.WIDTH * raster.getNumBands()];
        for (int y = 0; y < image.getHeight(); y++) {
            raster.getPixels(0, y, image.getWidth(), 1, row);
            for (int x = 0; x < image.getWidth(); x++) {
                int at = x * raster.getNumBands();
                if (model.hasAlpha()) P5gProfile.require(row[at + 3] == 255, "TRANSPARENT_PIXEL");
                rgb[offset++] = (byte) row[at]; rgb[offset++] = (byte) row[at + 1]; rgb[offset++] = (byte) row[at + 2];
            }
        }
        return rgb;
    }

    static void same(Decoded source, Decoded candidate) throws IOException {
        P5gProfile.require(Arrays.equals(source.rgb(), candidate.rgb()), "RGB_MISMATCH");
        source.metadata().same(candidate.metadata());
    }

    private static void verifyWebpConsumption(byte[] bytes, BufferedImage image) throws Exception {
        int start = 0, end = 0;
        for (int at = 12; at < bytes.length;) {
            int length = java.nio.ByteBuffer.wrap(bytes, at + 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            if (new String(bytes, at, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("VP8L")) {
                start = at + 8; end = start + length; break;
            }
            at += 8 + length + length % 2;
        }
        P5gProfile.require(start > 0, "WEBP_VP8L_REQUIRED");
        // Aynı pinli codec tekrar okunur; reader'ın chunk sonuna seek etmesi tüketim kanıtı değildir.
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            input.setByteOrder(java.nio.ByteOrder.LITTLE_ENDIAN); input.seek(start);
            Class<?> type = Class.forName("com.twelvemonkeys.imageio.plugins.webp.lossless.VP8LDecoder");
            Object decoder = type.getConstructor(javax.imageio.stream.ImageInputStream.class, boolean.class).newInstance(input, false);
            var raster = image.getRaster().createCompatibleWritableRaster();
            var byteRaster = Class.forName("com.twelvemonkeys.imageio.util.RasterUtils")
                    .getMethod("asByteRaster", java.awt.image.WritableRaster.class).invoke(null, raster);
            type.getMethod("readVP8Lossless", java.awt.image.WritableRaster.class, boolean.class,
                    javax.imageio.ImageReadParam.class, int.class, int.class)
                    .invoke(decoder, byteRaster, true, null, P5gProfile.WIDTH, P5gProfile.HEIGHT);
            var bitsField = type.getDeclaredField("lsbBitReader"); bitsField.setAccessible(true);
            Object bits = bitsField.get(decoder);
            var offsetField = bits.getClass().getDeclaredField("bitOffset"); offsetField.setAccessible(true);
            int offset = offsetField.getInt(bits);
            long consumedBits = Math.addExact(Math.multiplyExact(input.getStreamPosition(), 8), offset);
            P5gProfile.require(offset >= 0 && offset < 8 && (consumedBits + 7) / 8 == end,
                    "WEBP_PAYLOAD_TRAILING_OR_TRUNCATED");
        }
    }

    private static ImageReader reader(String mime) throws Exception {
        String spi = switch (mime) {
            case "image/webp" -> P5gProfile.SPI;
            case "image/png" -> "com.sun.imageio.plugins.png.PNGImageReaderSpi";
            case "image/jpeg" -> "com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi";
            default -> throw new IOException("MIME_REFUSED");
        };
        // JDK provider'ları module export gerektirmeden registry'den tam sınıf adıyla seçilir.
        if (!mime.equals("image/webp")) {
            var providers = javax.imageio.spi.IIORegistry.getDefaultInstance().getServiceProviders(ImageReaderSpi.class, false);
            while (providers.hasNext()) {
                var provider = providers.next(); if (provider.getClass().getName().equals(spi)) return provider.createReaderInstance();
            }
            throw new IOException("JDK_PROVIDER_MISSING");
        }
        var provider = (ImageReaderSpi) Class.forName(spi).getConstructor().newInstance();
        P5gProfile.require(provider.getVersion().equals("3.15.3"), "CODEC_VERSION_REFUSED");
        return provider.createReaderInstance();
    }
}
