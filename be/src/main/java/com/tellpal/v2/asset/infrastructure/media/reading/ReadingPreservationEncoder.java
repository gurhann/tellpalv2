package com.tellpal.v2.asset.infrastructure.media.reading;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.*;
import javax.imageio.stream.FileImageOutputStream;
import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;
import com.tellpal.v2.asset.domain.reading.ReadingProfiles;

/** Native encoder yalnız profilsiz canonical ham RGB PNG görür; metadata sonradan byte olarak eklenir. */
final class ReadingPreservationEncoder {
    private ReadingPreservationEncoder() { }

    static void encode(Path input, Path output, TestProfile profile) throws Exception {
        byte[] source = Files.readAllBytes(input); String mime = ReadingPreservation.mime(source);
        ReadingPreservation.require(!mime.equals("image/webp"), "SOURCE_PNG_OR_JPEG_REQUIRED");
        var original = ReadingPreservationRawDecoder.decode(source, mime, profile);
        ReadingPreservation.require(original.width() <= profile.width() && original.height() <= profile.height(), "PRESERVATION_GEOMETRY_LIMIT");
        if (mime.equals("image/jpeg")) {
            ReadingPreservation.require(source.length <= profile.maxOutputBytes(), "OUTPUT_BYTE_LIMIT");
            Files.write(output, source);
        } else {
            Path canonical = output.resolveSibling("canonical.png");
            var image = ReadingPreservation.image(original);
            var writer = ImageIO.getImageWritersByFormatName("PNG").next();
            try (var target = new FileImageOutputStream(canonical.toFile())) {
                writer.setOutput(target); var parameters = writer.getDefaultWriteParam();
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); parameters.setCompressionType("Deflate");
                parameters.setCompressionQuality(ReadingProfiles.PNG_COMPRESSION_QUALITY);
                parameters.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                writer.write(null, new IIOImage(image, null, null), parameters);
            } finally { writer.dispose(); image.flush(); }
            run(List.of(ReadingWebpTools.binary("cwebp").toString(), "-lossless", "-exact", "-q", "100", "-m", "6", "-metadata", "none",
                    canonical.toString(), "-o", output.toString()), output.getParent(), profile);
            var metadata = original.metadata();
            add(output, "icc", metadata.icc(), profile);
            add(output, "exif", metadata.tiff(), profile);
            add(output, "xmp", metadata.xmp(), profile);
        }
        ReadingPreservation.require(Files.size(output) > 0 && Files.size(output) <= profile.maxOutputBytes(), "OUTPUT_BYTE_LIMIT");
        ReadingPreservation.verify(source, Files.readAllBytes(output), profile);
    }

    private static void add(Path output, String kind, byte[] bytes, TestProfile profile) throws Exception {
        if (bytes == null) return;
        Path metadata = output.resolveSibling(kind + ".metadata"), muxed = output.resolveSibling(kind + ".webp");
        Files.write(metadata, bytes);
        run(List.of(ReadingWebpTools.binary("webpmux").toString(), "-set", kind, metadata.toString(), output.toString(), "-o", muxed.toString()), output.getParent(), profile);
        Files.move(muxed, output, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void run(List<String> command, Path directory, TestProfile profile) throws Exception {
        Path log = directory.resolve("native.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
        try {
            if (!process.waitFor(profile.sourceMillis(), TimeUnit.MILLISECONDS)) throw new IOException("NATIVE_ENCODER_TIMEOUT");
            if (process.exitValue() != 0) throw new IOException("NATIVE_ENCODER_REJECTED");
            if (Files.size(log) > ReadingImageProcess.LOG_BYTES) throw new IOException("CHILD_LOG_LIMIT");
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            process.waitFor();
        }
    }
}
