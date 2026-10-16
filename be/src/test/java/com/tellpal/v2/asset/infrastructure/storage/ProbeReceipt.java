package com.tellpal.v2.asset.infrastructure.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;

/** Exclusive ownership ledger, forced to disk before any upload and preserved on failure. */
final class ProbeReceipt {
    private final Path path;
    private ProbeReceipt(Path path) { this.path = path; }

    static ProbeReceipt create(ProbeConfiguration config, String key) throws IOException {
        config.privateLocation(config.receipt());
        try (var channel = FileChannel.open(config.receipt(), java.util.Set.of(StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))) {
            write(channel, "EVIDENCE LIVE_GCS\nWORKER " + ReadingStorageLiveProbe.class.getName() + "\nPROJECT " + config.project() + "\nBUCKET " + config.bucket()
                    + "\nOWNED_KEY " + key + "\nPLANNED_CREATE_ATTEMPTS 4\nRETAINED_OBJECTS_MAX 1\nSTATE PLANNED_NO_CLEANUP_NO_NEW_KEY_RETRY\n");
        }
        // Persist the exclusive directory entry before any upload can start.
        try (var parent = FileChannel.open(config.receipt().getParent(), StandardOpenOption.READ)) { parent.force(true); }
        return new ProbeReceipt(config.receipt());
    }

    void record(String status) throws IOException {
        try (var channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND, LinkOption.NOFOLLOW_LINKS)) {
            write(channel, status + "\n");
        }
    }

    private static void write(FileChannel channel, String text) throws IOException {
        var buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) channel.write(buffer);
        channel.force(true);
    }
}
