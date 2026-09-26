package com.tellpal.v2.asset.infrastructure.media;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
final class PageImageRenditionProcessor {

    private final int maxLongestEdge;
    private final long maxPixels;
    private final int qualityPercent;

    PageImageRenditionProcessor(
            @Value("${tellpal.asset.story-page-media.max-longest-edge:1440}") int maxLongestEdge,
            @Value("${tellpal.asset.story-page-media.max-pixels:40000000}") long maxPixels,
            @Value("${tellpal.asset.story-page-media.jpeg-quality-percent:85}") int qualityPercent) {
        if (maxLongestEdge <= 0 || maxPixels <= 0 || qualityPercent < 1 || qualityPercent > 100) {
            throw new IllegalArgumentException("Story page image profile limits are invalid");
        }
        this.maxLongestEdge = maxLongestEdge;
        this.maxPixels = maxPixels;
        this.qualityPercent = qualityPercent;
    }

    ProcessedPageImage process(byte[] sourceBytes, String sourceMimeType) {
        if (sourceBytes == null || sourceBytes.length == 0) {
            throw new IllegalArgumentException("Page illustration source is empty");
        }
        if (!"image/png".equalsIgnoreCase(sourceMimeType) && !"image/jpeg".equalsIgnoreCase(sourceMimeType)) {
            throw new IllegalArgumentException("Page illustration source MIME type is unsupported");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(sourceBytes))) {
            if (input == null) throw new IllegalArgumentException("Page illustration could not be decoded");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Page illustration format is unsupported");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int sourceWidth = reader.getWidth(0);
                int sourceHeight = reader.getHeight(0);
                if (sourceWidth <= 0 || sourceHeight <= 0 || (long) sourceWidth * sourceHeight > maxPixels) {
                    throw new IllegalArgumentException("Page illustration dimensions exceed the processing limit");
                }
                BufferedImage source = reader.read(0);
                if (source == null) throw new IllegalArgumentException("Page illustration could not be decoded");
                double scale = Math.min(1d, (double) maxLongestEdge / Math.max(sourceWidth, sourceHeight));
                int width = Math.max(1, (int) Math.round(sourceWidth * scale));
                int height = Math.max(1, (int) Math.round(sourceHeight * scale));
                BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, width, height);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.drawImage(source, 0, 0, width, height, null);
                } finally {
                    graphics.dispose();
                }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                var writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) throw new IllegalStateException("JPEG encoder is unavailable");
                var writer = writers.next();
                try (var outputStream = ImageIO.createImageOutputStream(bytes)) {
                    writer.setOutput(outputStream);
                    var parameters = writer.getDefaultWriteParam();
                    if (parameters.canWriteCompressed()) {
                        parameters.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                        parameters.setCompressionQuality(qualityPercent / 100f);
                    }
                    writer.write(null, new javax.imageio.IIOImage(output, null, null), parameters);
                } finally {
                    writer.dispose();
                }
                return new ProcessedPageImage(bytes.toByteArray(), width, height, "image/jpeg");
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Page illustration could not be decoded or encoded", exception);
        }
    }

    String profileVersion() {
        return "story-page-jpeg-v1-edge-%d-quality-%d".formatted(maxLongestEdge, qualityPercent);
    }

    boolean isDecodable(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    record ProcessedPageImage(byte[] bytes, int width, int height, String mimeType) {
    }
}
