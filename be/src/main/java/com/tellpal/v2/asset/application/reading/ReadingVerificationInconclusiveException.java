package com.tellpal.v2.asset.application.reading;

import com.tellpal.v2.asset.domain.reading.ReadingDeadline.ReadingDeadlineExceededException;

/** Resource admission or elapsed time provides no observation that stored media is corrupt. */
public final class ReadingVerificationInconclusiveException extends IllegalStateException {
    public ReadingVerificationInconclusiveException(String message) {
        super(message);
    }

    public static boolean causedByResourceRejection(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ReadingVerificationInconclusiveException
                    || cause instanceof ReadingDeadlineExceededException
                    || cause instanceof java.io.InterruptedIOException) return true;
        }
        return false;
    }
}
