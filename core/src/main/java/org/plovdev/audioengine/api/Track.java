package org.plovdev.audioengine.api;

import org.plovdev.audioengine.exceptions.AudioEngineException;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.metadata.TrackMetadata;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;

/**
 * Most important AudioSND class.
 * The class is the fundamental unit of work with the engine.
 *
 * @author Anton
 * @version 1.0
 */
public class Track {
    // Track information
    private final ByteBuffer trackData;
    private final Duration duration;
    private final AudioFormat format;
    private TrackMetadata metaData;

    /**
     * Create audio track with DIRECT ByteBuffer.
     *
     * @param trackData track's memory segment
     * @param duration  audio duration
     * @param format    audio track format
     * @param metaData  metadata, loaded from file, or created by hands
     * @throws NullPointerException if trackData or format is null
     * @throws AudioEngineException if trackData is not a direct buffer
     */
    public Track(ByteBuffer trackData, Duration duration, AudioFormat format, TrackMetadata metaData) {
        Objects.requireNonNull(trackData, "trackData must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        Objects.requireNonNull(format, "format must not be null");

        this.trackData = trackData.asReadOnlyBuffer(); // Read-only wrapper
        this.duration = duration;
        this.format = format;
        this.metaData = metaData; // Can be null
    }

    /**
     * Returns read-only view of track audio data.
     * <p>
     * Buffer is {@link ByteBuffer#isReadOnly() read-only} and direct.
     * Position and limit should not be modified — use {@link ByteBuffer#duplicate()}
     * if you need to manipulate position.
     * </p>
     */
    public ByteBuffer getTrackData() {
        return trackData;
    }

    public Duration getDuration() {
        return duration;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public TrackMetadata getMetaData() {
        return metaData;
    }

    /**
     * Sets track metadata.
     * <p>
     * Useful for updating tags before export or after editing.
     * </p>
     *
     * @param metaData new metadata (can be null)
     */
    public void setMetaData(TrackMetadata metaData) {
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "Track{" +
                "format=" + format +
                ", duration=" + duration +
                ", trackData=" + trackData +
                '}';
    }
}