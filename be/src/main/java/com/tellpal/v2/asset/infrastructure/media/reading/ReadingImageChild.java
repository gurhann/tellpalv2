package com.tellpal.v2.asset.infrastructure.media.reading;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;

/** Isolated bounded ImageIO worker. Every role is rendered directly from retained original bytes. */
public final class ReadingImageChild {
    private ReadingImageChild() { }
    private static final class BoundedImageOutput extends FileImageOutputStream {
        private final long limit;
        BoundedImageOutput(File file,long limit) throws IOException { super(file); this.limit=limit; }
        @Override public void write(int value) throws IOException {
            if(getStreamPosition()>=limit) throw new IOException("OUTPUT_BYTE_LIMIT"); super.write(value);
        }
        @Override public void write(byte[] bytes,int offset,int length) throws IOException {
            if(getStreamPosition()+length>limit) throw new IOException("OUTPUT_BYTE_LIMIT"); super.write(bytes,offset,length);
        }
    }
    public static void main(String[] args) throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var profile = mapper.readValue(Files.readString(Path.of(args[2])), TestProfile.class);
        var actual=com.tellpal.v2.asset.domain.reading.ReadingProfiles.capture(com.tellpal.v2.asset.api.ReadingImagePreparationApi.Role.READING_PAGE,profile);
        if(args.length!=4 || !actual.fingerprint().equals(args[3])) throw new IOException("CHILD_ENCODER_RUNTIME_IDENTITY_MISMATCH");
        if (Files.size(Path.of(args[0])) > profile.maxSourceBytes()) throw new IOException("SOURCE_BYTE_LIMIT");
        var source = ReadingImageInspector.decode(Files.readAllBytes(Path.of(args[0])),profile).image();
        if(source.getWidth()<profile.width() || source.getHeight()<profile.height()) throw new IOException("SOURCE_SHORTAGE_NO_UPSCALE");
        double factor=Math.min((double)profile.width()/source.getWidth(),(double)profile.height()/source.getHeight());
        int width=Math.max(1,(int)Math.floor(source.getWidth()*factor)), height=Math.max(1,(int)Math.floor(source.getHeight()*factor));
        var result=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        var graphics=result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics.drawImage(source,0,0,width,height,null);
        } finally { graphics.dispose(); }
        var writer=ImageIO.getImageWritersByFormatName("PNG").next();
        try(var output=new BoundedImageOutput(Path.of(args[1]).toFile(),profile.maxOutputBytes())) {
            writer.setOutput(output); var parameters=writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); parameters.setCompressionType("Deflate"); parameters.setCompressionQuality(1.0f);
            parameters.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
            writer.write(null,new IIOImage(result,null,null),parameters);
        } finally { writer.dispose(); }
        if(Files.size(Path.of(args[1]))>profile.maxOutputBytes()) throw new IOException("OUTPUT_BYTE_LIMIT");
    }
}
