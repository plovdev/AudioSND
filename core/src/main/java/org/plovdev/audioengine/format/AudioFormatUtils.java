package org.plovdev.audioengine.format;

import org.jspecify.annotations.NonNull;

public final class AudioFormatUtils {
    private AudioFormatUtils() {
    }

    public static long calculateTrackDuration(@NonNull AudioFormat format, long sizeInBytes) {
        long sampleRate = format.sampleRate();
        long channels = format.channels();

        // Bytes per second = samples/sec * bytes/sample * channels
        long bytesPerSecond = (sampleRate * 4 * channels);

        if (bytesPerSecond == 0) {
            return 0; // Avoid division by zero
        }

        // Duration in ms = (bytes * 1000) / (bytes/sec)
        return (sizeInBytes * 1000L) / bytesPerSecond;
    }
}
