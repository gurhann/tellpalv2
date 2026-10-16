package com.tellpal.v2.asset.domain.reading;

import java.util.concurrent.TimeUnit;

/** One transient monotonic revision budget, never part of source/profile identity or persisted work. */
public final class ReadingDeadline {
    private final long started;
    private final long duration;
    private ReadingDeadline(long started,long millis) {
        if(millis<=0) throw new IllegalArgumentException("Positive revision duration required");
        this.started=started;this.duration=TimeUnit.MILLISECONDS.toNanos(millis);
    }
    public static ReadingDeadline start(long millis) { return since(System.nanoTime(),millis); }
    public static ReadingDeadline since(long started,long millis) { return new ReadingDeadline(started,millis); }
    public void check() { remainingMillis(Long.MAX_VALUE); }
    public long remainingMillis(long operationMillis) {
        long remaining=duration-(System.nanoTime()-started);
        long millis=TimeUnit.NANOSECONDS.toMillis(remaining);
        if(millis<=0) throw new ReadingDeadlineExceededException();
        return Math.min(operationMillis,millis);
    }
    public static final class ReadingDeadlineExceededException extends RuntimeException {
        public ReadingDeadlineExceededException() { super("REVISION_DURATION_LIMIT"); }
    }
}
