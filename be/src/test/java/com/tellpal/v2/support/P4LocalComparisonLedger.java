package com.tellpal.v2.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.util.*;

/** Başarısızlık ve yeniden başlatmada geri verilmeyen, çağrı öncesi fsync rezervasyonu. */
final class P4LocalComparisonLedger implements AutoCloseable {
    static final String PLAN_SHA = "f024f17075a11cac9e61019503736cd384ac6ecf0db3236052c62df9fd439be3";
    static final String RUN = "p4a2-cb5fe81a-7ada-4cb5-adce-6ddde1381d09";
    static final String PREFIX = "local/" + RUN;
    static final long MIB = 1048576;
    private static final int[][] PHASES = {{3,3,0},{3,9,3},{4,8,5},{4,12,0},{0,8,0},{0,24,0}};
    private final FileChannel channel;
    private final Clock clock;
    private final long started;
    private final Map<String,Long> objects = new HashMap<>();
    private final Set<String> once = new HashSet<>();
    private final Set<String> duplicateCreates = new HashSet<>();
    private int phase = -1, creates, reads, observations, verifications;
    private long writeBytes, readBytes, objectBytes;
    private int phaseCreates, phaseReads, phaseObservations;
    private boolean failed;

    P4LocalComparisonLedger(Path path, Clock clock) throws IOException {this(path,clock,0);}

    P4LocalComparisonLedger(Path path, Clock clock, int loginVerifications) throws IOException {
        this.clock = clock;
        this.started = clock.millis();
        channel = FileChannel.open(path, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        append("OPEN " + PLAN_SHA + " " + started);
        require(loginVerifications==0 || loginVerifications==1,"LOGIN_RESERVATION");
        verifications=loginVerifications;
        if(loginVerifications==1)append("AUTH_LOGIN_RESERVED 1");
    }

    synchronized void phase(int next) {
        require(next == phase + 1 && next < PHASES.length, "PHASE_REPLAY_OR_ORDER");
        // Aşamanın en kötü payı başlamadan kalıcı ayrılır. Harcanmayan pay sonraki aşamaya aktarılmaz.
        int[] cap = PHASES[next];
        append("PHASE_RESERVED " + next + " " + cap[0] + " " + cap[1] + " " + cap[2]
                + " " + cap[0] * 8 * MIB + " " + cap[1] * 8 * MIB);
        phase = next;
        phaseCreates = phaseReads = phaseObservations = 0;
    }

    synchronized void once(String action) {
        require(once.add(action), "ACTION_REPLAY");
        append("ACTION " + action);
    }

    synchronized void scope(long id) {
        require(id > 0 && id != 71 && id != 93 && id != 112, "FIXTURE_SCOPE");
        once("SEAL_CONTENT_ID");
        append("SEALED_CONTENT " + id);
    }

    synchronized void read(String path, long bytes) {
        path(path);
        require(phase >= 0 && ++phaseReads <= PHASES[phase][1] && ++reads <= 64
                && bytes > 0 && bytes <= 8 * MIB && readBytes + bytes <= 512 * MIB, "READ_BUDGET");
        readBytes += bytes;
        append("READ_RESERVED " + reads + " " + readBytes + " " + path);
    }

    synchronized void create(String path, long bytes) {
        path(path);
        require(phase >= 0 && ++phaseCreates <= PHASES[phase][0] && ++creates <= 14
                && bytes > 0 && bytes <= 8 * MIB && writeBytes + bytes <= 112 * MIB, "CREATE_BUDGET");
        writeBytes += bytes;
        if (!objects.containsKey(path)) {
            require(objects.size() < 13 && objectBytes + bytes <= 104 * MIB, "OBJECT_BUDGET");
            objects.put(path, bytes);
            objectBytes += bytes;
        } else {
            require(phase == 2 && Objects.equals(objects.get(path), bytes), "UNEXPECTED_DUPLICATE_CREATE");
            duplicateCreates.add(path);
        }
        append("CREATE_RESERVED " + creates + " " + writeBytes + " " + objects.size() + " " + objectBytes + " " + path);
    }

    synchronized void observe(String path) {
        path(path);
        require(phase >= 0 && ++phaseObservations <= PHASES[phase][2] && ++observations <= 16, "OBSERVE_BUDGET");
        append("OBSERVE_RESERVED " + observations + " " + path);
    }

    synchronized void conflict(String path) {
        require(phase == 2 && duplicateCreates.remove(path), "UNEXPECTED_CONFLICT");
        once("EXPECTED_CAPTURE_CONFLICT");
        observe(path);
    }

    synchronized void verify() {
        require(phase == 5 && ++verifications <= 64, "AUTH_BUDGET");
        append("AUTH_RESERVED " + verifications);
    }

    synchronized void record(String key, String value) {
        require(key.matches("[A-Z_]+") && value != null && !value.contains("\n") && !value.contains("\r"), "RECEIPT_VALUE");
        append(key + " " + value);
    }

    private void path(String path) {
        require(path != null && path.startsWith(PREFIX + "/") && !path.contains("..") && !path.contains("\\"), "STORAGE_SCOPE");
    }

    private void append(String line) {
        require(!failed && clock.millis() >= started && clock.millis() - started < 900000, "RUN_WINDOW_OR_FAILED");
        try {
            var data = ByteBuffer.wrap((line + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            while (data.hasRemaining()) channel.write(data);
            channel.force(true);
        } catch (IOException e) {
            failed = true;
            throw new IllegalStateException("LEDGER_WRITE_FAILED", e);
        }
    }

    static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException(code);
    }

    synchronized long remainingMillis() {return Math.max(0,900000-(clock.millis()-started));}

    @Override public void close() throws IOException { channel.close(); }
}
