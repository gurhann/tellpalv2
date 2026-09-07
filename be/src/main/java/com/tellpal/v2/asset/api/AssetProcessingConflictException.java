package com.tellpal.v2.asset.api;

/**
 * Signals that a processing lifecycle operation conflicts with the current job state.
 *
 * <p>This exception is part of the asset module API so callers in other modules can map the
 * conflict without depending on asset application implementation classes.
 */
public class AssetProcessingConflictException extends RuntimeException {

    protected AssetProcessingConflictException(String message) {
        super(message);
    }
}
