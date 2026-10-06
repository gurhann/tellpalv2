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

public final class ListingCoverRenditionProcessor {

    private final int maxWidth;
    private final int maxHeight;
    private final long maxPixels;
    private final int qualityPercent;

    public ListingCoverRenditionProcessor(int maxWidth,int maxHeight,long maxPixels,int qualityPercent) {
        if(maxWidth<=0 || maxHeight<=0 || maxPixels<=0 || qualityPercent<1 || qualityPercent>100)
            throw new IllegalArgumentException("Listing image profile limits are invalid");
        this.maxWidth=maxWidth;this.maxHeight=maxHeight;this.maxPixels=maxPixels;this.qualityPercent=qualityPercent;
    }

    public ProcessedPageImage process(byte[] sourceBytes, String sourceMimeType) {
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
                String decodedFormat = reader.getFormatName();
                if (!("image/png".equalsIgnoreCase(sourceMimeType) && "png".equalsIgnoreCase(decodedFormat))
                        && !("image/jpeg".equalsIgnoreCase(sourceMimeType) && ("jpeg".equalsIgnoreCase(decodedFormat) || "jpg".equalsIgnoreCase(decodedFormat))))
                    throw new IllegalArgumentException("Declared image MIME does not match decoding");
                int sourceWidth = reader.getWidth(0);
                int sourceHeight = reader.getHeight(0);
                if (sourceWidth <= 0 || sourceHeight <= 0 || (long) sourceWidth * sourceHeight > maxPixels) {
                    throw new IllegalArgumentException("Page illustration dimensions exceed the processing limit");
                }
                BufferedImage source = reader.read(0);
                if (source == null) throw new IllegalArgumentException("Page illustration could not be decoded");
                double scale = Math.min(1d, Math.min((double) maxWidth/sourceWidth,(double) maxHeight/sourceHeight));
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

    public boolean isDecodable(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (IOException exception) {
            return false;
        }
    }

    public record ProcessedPageImage(byte[] bytes, int width, int height, String mimeType) {
    }
}
