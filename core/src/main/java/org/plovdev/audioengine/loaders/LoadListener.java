package org.plovdev.audioengine.loaders;

import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;

/**
 * Listener for track loading events.
 * <p>
 * Implement this interface to receive notifications about
 * asynchronous track loading operations.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @since 1.0
 */
public interface LoadListener {

    /**
     * Called when loading starts.
     *
     * @param total total size in bytes, or {@code -1} if unknown
     */
    default void onLoadStarted(long total) {
    }

    /**
     * Called when loading completes successfully.
     */
    default void onLoadFinished() {
    }

    /**
     * Called when loading fails.
     *
     * @param error detailed exception describing the failure
     */
    default void onLoadFailed(TrackLoadException error) {
    }
}