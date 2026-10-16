import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;

public class ImageIoPreflight {
  static final int WIDTH=257, HEIGHT=193;
  static String sha(byte[] bytes) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
  static void require(boolean predicate, String message) {if(!predicate) throw new IllegalStateException(message);}
  static BufferedImage fixture() {
    var image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
    for(int y=0;y<HEIGHT;y++) for(int x=0;x<WIDTH;x++)
      image.setRGB(x,y, (((x*3+y*2)&255)<<16)|(((x*7+y*5)&255)<<8)|((x*13+y*11)&255));
    return image;
  }
  static void verify(String format, Path path, byte[] expected, boolean pixels) throws Exception {
    byte[] actual=Files.readAllBytes(path);
    require(actual.length==expected.length && MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(actual),MessageDigest.getInstance("SHA-256").digest(expected)),"Stored byte/hash mismatch");
    try(var input=new FileImageInputStream(path.toFile())) {
      var readers=ImageIO.getImageReaders(input); require(readers.hasNext(),"No decoder");var reader=readers.next();
      try {
        reader.setInput(input,false,false);
        require(reader.getFormatName().equalsIgnoreCase(format),"Actual format differs");
        require(reader.getWidth(0)==WIDTH && reader.getHeight(0)==HEIGHT,"Stored dimensions differ");
        var decoded=reader.read(0);require(decoded!=null,"Decode failed");
        if(pixels) {var reference=fixture();for(int y=0;y<HEIGHT;y++)for(int x=0;x<WIDTH;x++) require(decoded.getRGB(x,y)==reference.getRGB(x,y),"PNG RGB pixel mismatch");}
        System.out.println("VERIFIED format="+format+" mime=image/"+format+" bytes="+actual.length+" sha256="+sha(actual)+" width="+WIDTH+" height="+HEIGHT+" reader="+reader.getClass().getName()+" readerProvider="+reader.getOriginatingProvider().getVendorName()+"/"+reader.getOriginatingProvider().getVersion()+" exactRgbPixels="+pixels);
      } finally {reader.dispose();}
    }
  }
  public static void main(String[] args) throws Exception {
    var root=Path.of(args[0]);System.out.println("SCOPE synthetic-local-runtime-availability-only; no-production-profile-or-visual-quality-acceptance");
    System.out.println("RUNTIME "+System.getProperty("java.runtime.version")+" vendor="+System.getProperty("java.vendor")+" vm="+System.getProperty("java.vm.name")+" os="+System.getProperty("os.name")+" arch="+System.getProperty("os.arch"));
    for(String format:List.of("png","jpeg")) {
      var writers=ImageIO.getImageWritersByFormatName(format);require(writers.hasNext(),"Writer unavailable");var writer=writers.next();var encoded=new ByteArrayOutputStream();
      System.out.println("ENCODER format="+format+" writer="+writer.getClass().getName()+" writerProvider="+writer.getOriginatingProvider().getVendorName()+"/"+writer.getOriginatingProvider().getVersion());
      try(var output=new MemoryCacheImageOutputStream(encoded)) {
        writer.setOutput(output);var parameter=writer.getDefaultWriteParam();
        if(format.equals("jpeg")) {parameter.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);parameter.setCompressionQuality(.85f);parameter.setProgressiveMode(ImageWriteParam.MODE_DISABLED);}
        writer.write(null,new IIOImage(fixture(),null,null),parameter);
      } finally {writer.dispose();}
      byte[] bytes=encoded.toByteArray();var path=root.resolve("fixture."+format);Files.write(path,bytes,StandardOpenOption.CREATE_NEW);
      verify(format,path,bytes,format.equals("png"));
      var missing=root.resolve("missing."+format);boolean rejected=false;
      try {verify(format,missing,bytes,false);}catch(NoSuchFileException expected){rejected=true;}
      require(rejected,"Missing output admitted");System.out.println("NEGATIVE format="+format+" missing=REJECTED");
      var truncated=root.resolve("truncated."+format);Files.write(truncated,Arrays.copyOf(bytes,bytes.length/2),StandardOpenOption.CREATE_NEW);rejected=false;
      try {verify(format,truncated,bytes,false);}catch(IllegalStateException expected){rejected=true;}
      require(rejected,"Truncated output admitted");System.out.println("NEGATIVE format="+format+" truncated=REJECTED_BY_INDEPENDENT_BYTE_HASH_CHECK");
    }
    System.out.println("RESULT PASS two persisted formats, two fresh readers, PNG exact RGB, four negative cases");
  }
}
