package com.tellpal.v2.asset.application.reading;

import java.util.concurrent.Semaphore;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Bounds parent capture/decode and child work jointly within this backend process. */
public final class ReadingPreparationCapacity implements AutoCloseable {
    private static final Semaphore CAPACITY = new Semaphore(1, true);
    private ReadingPreparationCapacity() { }
    public static ReadingPreparationCapacity acquire() {
        requireNoTransaction();
        if (!CAPACITY.tryAcquire()) throw new IllegalStateException("READING_PARENT_CAPACITY_EXHAUSTED");
        return new ReadingPreparationCapacity();
    }
    /** Admission reserves at most one quarter of parent max heap and half of currently allocatable heap. */
    public static void requireParentMemory(com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile profile) {
        Runtime runtime=Runtime.getRuntime();
        long available=runtime.maxMemory()-(runtime.totalMemory()-runtime.freeMemory());
        if(profile.parentMemoryBytes()>runtime.maxMemory()/4 || profile.parentMemoryBytes()>available/2)
            throw new IllegalStateException("READING_PARENT_HEAP_ADMISSION_EXCEEDED");
    }
    public static void requireNoTransaction() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("READING_IO_INSIDE_TRANSACTION");
    }
    @Override public void close() { CAPACITY.release(); }
}
