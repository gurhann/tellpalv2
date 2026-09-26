package com.tellpal.v2.asset.infrastructure.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class PageImageRenditionProcessorTest {

    private final PageImageRenditionProcessor processor = new PageImageRenditionProcessor(64, 100_000, 85);

    @Test
    void convertsGeneratedPngFixtureToBoundedDecodableJpegWithoutUpscaling() throws Exception {
        BufferedImage source = new BufferedImage(160, 80, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        var result = processor.process(png.toByteArray(), "image/png");
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(result.bytes()));

        assertThat(result.mimeType()).isEqualTo("image/jpeg");
        assertThat(result.width()).isEqualTo(64);
        assertThat(result.height()).isEqualTo(32);
        assertThat(decoded).isNotNull();
        assertThat(processor.profileVersion()).contains("edge-64", "quality-85");
    }

    @Test
    void leavesSmallerInputAtSourceDimensions() throws Exception {
        BufferedImage source = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        var result = processor.process(png.toByteArray(), "image/png");

        assertThat(result.width()).isEqualTo(20);
        assertThat(result.height()).isEqualTo(10);
    }

    @Test
    void rejectsUnsupportedMimeAndCorruptInput() {
        assertThatThrownBy(() -> processor.process(new byte[] {1, 2, 3}, "image/webp"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> processor.process(new byte[] {1, 2, 3}, "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
