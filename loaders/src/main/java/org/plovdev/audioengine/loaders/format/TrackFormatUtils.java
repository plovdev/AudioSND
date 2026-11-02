package org.plovdev.audioengine.loaders.format;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.TrackFormat;

/**
 * Utility class for common operations with {@link TrackFormat}.
 * <p>
 * Provides factory methods for quality presets, file size calculations,
 * duration estimation and chunk size determination.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see TrackFormat
 */
public final class TrackFormatUtils {
    private TrackFormatUtils() {
        // Utility class, no instances
    }

    /**
     * Estimates file size for the given format and duration.
     * <p>
     * For PCM formats uses sample rate, a bit of depth and channel count.
     * For compressed formats uses {@link TrackFormat#bitRate()} which may be
     * estimated or fixed (e.g., MP3 with constant bitrate).
     * </p>
     *
     * @param format          track format
     * @param durationSeconds duration in seconds
     * @return estimated file size in bytes
     */
    public static long calculateFileSize(@org.jspecify.annotations.NonNull TrackFormat format, long durationSeconds) {
        if (format.bitRate() > 0) {
            // For formats with known/estimated bitrate
            return (format.bitRate() * durationSeconds) / 8;
        } else {
            // For uncompressed PCM formats
            return format.sampleRate() * format.bitDepth() * format.channels() * durationSeconds / 8;
        }
    }

    /**
     * Calculates audio duration in milliseconds from file size.
     * <p>
     * Works correctly only for uncompressed PCM formats.
     * For compressed formats the result will be inaccurate.
     * </p>
     *
     * @param format      track format (must be PCM)
     * @param sizeInBytes file size in bytes
     * @return duration in milliseconds, or 0 if calculation fails
     */
    public static long calculateDurationMs(@NonNull TrackFormat format, int sizeInBytes) {
        long sampleRate = format.sampleRate();
        long bitsPerSample = format.bitDepth();
        long channels = format.channels();

        // Bytes per second = samples/sec * bytes/sample * channels
        long bytesPerSecond = (sampleRate * (bitsPerSample / 8) * channels);

        if (bytesPerSecond == 0) {
            return 0; // Avoid division by zero
        }

        // Duration in ms = (bytes * 1000) / (bytes/sec)
        return (sizeInBytes * 1000L) / bytesPerSecond;
    }

    /**
     * Returns the size of one audio frame in bytes for the given format.
     * <p>
     * One frame represents one sample across all channels.
     * </p>
     *
     * @param format track format
     * @return frame size in bytes
     */
    public static int calculateFrameSize(@NonNull TrackFormat format) {
        int bytesPerSample = format.bitDepth() / 8;
        int bytesPerFrame = bytesPerSample * format.channels();
        int framesPerMs = Math.max(1, format.sampleRate() / 1000);

        return (framesPerMs * bytesPerFrame);
    }
}