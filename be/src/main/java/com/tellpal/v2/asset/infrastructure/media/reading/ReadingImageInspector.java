package com.tellpal.v2.asset.infrastructure.media.reading;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;

/** Rejects unsupported container semantics before bounded full-raster decode; decoder warnings are failures. */
public final class ReadingImageInspector {
    private ReadingImageInspector() { }
    public record Decoded(BufferedImage image, String mime) { }
    public static Decoded decode(byte[] bytes, TestProfile profile) throws IOException {
        if (bytes.length == 0 || bytes.length > Math.max(profile.maxSourceBytes(), profile.maxOutputBytes()))
            throw new IOException("ENCODED_BYTE_LIMIT");
        if (ReadingPreservation.enabled(profile)) return ReadingPreservation.decode(bytes, profile);
        String mime = bytes.length >= 8 && bytes[0] == (byte)137 ? png(bytes,profile) : jpeg(bytes);
        ImageIO.setUseCache(false);
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("UNSUPPORTED_IMAGE");
            var reader = readers.next();
            try {
                reader.setInput(stream, false, false);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || Math.multiplyExact((long)width,height) > profile.maxPixels())
                    throw new IOException("FULL_RASTER_PIXEL_LIMIT");
                if (reader.getNumImages(true) != 1) throw new IOException("FRAME_LIMIT");
                reader.addIIOReadWarningListener((r, warning) -> { throw new IllegalArgumentException("MALFORMED_IMAGE:" + warning); });
                BufferedImage image = reader.read(0);
                if (image == null || image.getColorModel().hasAlpha() || !image.getColorModel().getColorSpace().isCS_sRGB())
                    throw new IOException("OPAQUE_SRGB_REQUIRED");
                for (int size : image.getColorModel().getComponentSize()) if (size != 8) throw new IOException("EIGHT_BIT_REQUIRED");
                return new Decoded(image,mime);
            } finally { reader.dispose(); }
        } catch (IllegalArgumentException | ArithmeticException e) { throw new IOException("MALFORMED_IMAGE",e); }
    }
    private static String png(byte[] bytes,TestProfile profile) throws IOException {
        byte[] signature = {(byte)137,80,78,71,13,10,26,10};
        if (bytes.length < 33 || !Arrays.equals(signature,Arrays.copyOf(bytes,8))) throw new IOException("PNG_SIGNATURE");
        int offset=8, metadata=0, width=0,height=0; boolean header=false, data=false, end=false, dataEnded=false, srgb=false;
        var compressed=new ByteArrayOutputStream();
        while (offset < bytes.length) {
            if (bytes.length-offset<12) throw new IOException("PNG_TRUNCATED");
            int length=ByteBuffer.wrap(bytes,offset,4).getInt();
            if(length<0 || (long)offset+length+12>bytes.length) throw new IOException("PNG_CHUNK_LENGTH");
            String type=new String(bytes,offset+4,4,java.nio.charset.StandardCharsets.US_ASCII);
            CRC32 crc=new CRC32(); crc.update(bytes,offset+4,length+4);
            if ((int)crc.getValue()!=ByteBuffer.wrap(bytes,offset+8+length,4).getInt()) throw new IOException("PNG_CRC");
            switch(type) {
                case "IHDR" -> {
                    if(header || offset!=8 || length!=13 || bytes[offset+16]!=8 || bytes[offset+17]!=2
                            || bytes[offset+18]!=0 || bytes[offset+19]!=0 || bytes[offset+20]!=0) throw new IOException("PNG_UNSUPPORTED_HEADER");
                    width=ByteBuffer.wrap(bytes,offset+8,4).getInt(); height=ByteBuffer.wrap(bytes,offset+12,4).getInt();
                    if(width<=0 || height<=0 || (long)width*height>profile.maxPixels()) throw new IOException("FULL_RASTER_PIXEL_LIMIT");
                    header=true;
                }
                case "IDAT" -> { if(!header || end || dataEnded) throw new IOException("PNG_CHUNK_ORDER"); data=true; compressed.write(bytes,offset+8,length); }
                case "IEND" -> { if(length!=0 || !data || end) throw new IOException("PNG_INVALID_END"); end=true; }
                case "sRGB" -> { if(srgb || data || length!=1 || Byte.toUnsignedInt(bytes[offset+8])>3) throw new IOException("PNG_COLOR"); metadata+=length; srgb=true; }
                default -> throw new IOException("PNG_UNSUPPORTED_CHUNK:"+type);
            }
            if (data && !type.equals("IDAT")) dataEnded=true;
            offset+=length+12;
            if(end && offset!=bytes.length) throw new IOException("PNG_TRAILING_BYTES");
            if(metadata>65536) throw new IOException("METADATA_LIMIT");
        }
        if(!end) throw new IOException("PNG_MISSING_END");
        validateDeflate(compressed.toByteArray(),width,height);
        return "image/png";
    }
    private static void validateDeflate(byte[] encoded,int width,int height) throws IOException {
        var inflater=new java.util.zip.Inflater();
        try {
            inflater.setInput(encoded); byte[] buffer=new byte[8192]; long count=0,expected=((long)width*3+1)*height;
            while(!inflater.finished()) {
                int size=inflater.inflate(buffer);
                if(size==0) throw new IOException("PNG_INVALID_DEFLATE");
                count+=size; if(count>expected) throw new IOException("PNG_RASTER_LENGTH");
            }
            if(count!=expected || inflater.getRemaining()!=0) throw new IOException("PNG_RASTER_LENGTH_OR_TRAILING_DEFLATE");
        } catch(java.util.zip.DataFormatException e) { throw new IOException("PNG_INVALID_DEFLATE",e); }
        finally { inflater.end(); }
    }
    private static String jpeg(byte[] bytes) throws IOException {
        if(bytes.length<4 || (bytes[0]&255)!=255 || (bytes[1]&255)!=216) throw new IOException("PNG_OR_JPEG_REQUIRED");
        int offset=2, metadata=0, frames=0; boolean scan=false, ended=false;
        while(offset<bytes.length) {
            if((bytes[offset++]&255)!=255) { if(scan) continue; throw new IOException("JPEG_MARKER"); }
            while(offset<bytes.length && (bytes[offset]&255)==255) offset++;
            if(offset>=bytes.length) throw new IOException("JPEG_TRUNCATED");
            int marker=bytes[offset++]&255;
            if(scan && (marker==0 || marker>=208 && marker<=215)) continue;
            if(marker==217) { ended=true; break; }
            if(offset+2>bytes.length) throw new IOException("JPEG_TRUNCATED");
            int length=((bytes[offset]&255)<<8)|(bytes[offset+1]&255);
            if(length<2 || offset+length>bytes.length) throw new IOException("JPEG_SEGMENT_LENGTH");
            if(marker>=192 && marker<=207 && marker!=196 && marker!=200 && marker!=204) {
                if(marker!=192 || ++frames!=1 || length<8 || bytes[offset+2]!=8 || bytes[offset+7]!=3)
                    throw new IOException("JPEG_BASELINE_8BIT_RGB_REQUIRED");
            } else if(marker>=224 && marker<=239 || marker==254) {
                metadata+=length;
                if(metadata>65536) throw new IOException("METADATA_LIMIT");
                if(marker!=224 || length<7 || !new String(bytes,offset+2,5,java.nio.charset.StandardCharsets.US_ASCII).equals("JFIF\0"))
                    throw new IOException("JPEG_UNSUPPORTED_METADATA_OR_ORIENTATION");
            } else if(marker!=196 && marker!=219 && marker!=221 && marker!=218) throw new IOException("JPEG_UNSUPPORTED_MARKER");
            if(marker==218) { if(frames!=1) throw new IOException("JPEG_FRAME_REQUIRED"); scan=true; }
            offset+=length;
        }
        if(!ended || !scan || offset!=bytes.length) throw new IOException("JPEG_MISSING_END_OR_TRAILING_BYTES");
        return "image/jpeg";
    }
}
