package com.tellpal.v2.asset.infrastructure.media;
import static org.assertj.core.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
class ListingCoverRenditionProcessorTest {
    @Test void fitsBothBoundsWithoutUpscalingAndCompositesAlphaWhite() throws Exception {
        byte[] small=png(80,120);var image=new ListingCoverRenditionProcessor(360,480,40000000,85).process(small,"image/png");
        assertThat(image.width()).isEqualTo(80);assertThat(image.height()).isEqualTo(120);
        assertThat(ImageIO.read(new ByteArrayInputStream(image.bytes())).getRGB(0,0)&0xffffff).isEqualTo(0xffffff);
        var large=new ListingCoverRenditionProcessor(360,480,40000000,85).process(png(1000,1500),"image/png");
        assertThat(large.width()).isEqualTo(320);assertThat(large.height()).isEqualTo(480);
        var tablet=new ListingCoverRenditionProcessor(600,800,40000000,85).process(png(1000,1500),"image/png");
        assertThat(tablet.width()).isEqualTo(533);assertThat(tablet.height()).isEqualTo(800);
    }
    @Test void rejectsDeclaredMimeMismatchAndExcessPixelsBeforeDecode() throws Exception {
        var processor=new ListingCoverRenditionProcessor(360,480,100,85);
        assertThatThrownBy(()->processor.process(png(20,20),"image/png")).hasMessageContaining("dimensions");
        assertThatThrownBy(()->processor.process(png(2,2),"image/jpeg")).hasMessageContaining("MIME");
        assertThatThrownBy(()->processor.process(new byte[]{1,2,3},"image/png")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void sourceProfilesAreCapturedAndRoundTrip() {
        var p=new ListingCoverProfile(360,480,600,800,85,20971520,40000000);
        assertThat(ListingCoverProfile.parse(p.version())).isEqualTo(p);
        assertThat(new ListingCoverProfile(320,480,600,800,85,20971520,40000000).version()).isNotEqualTo(p.version());
    }
    private byte[] png(int w,int h) throws Exception {var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB),"png",bytes);return bytes.toByteArray();}
}
