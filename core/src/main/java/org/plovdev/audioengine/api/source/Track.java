package org.plovdev.audioengine.api.source;

import org.plovdev.audioengine.exceptions.AudioEngineException;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.metadata.TrackMetadata;

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
    private final AudioSource audioSource;
    private final Duration duration;
    private final AudioFormat format;
    private TrackMetadata metaData;

    /**
     * Create audio track with DIRECT ByteBuffer.
     *
     * @param audioSource - the source of audio
     * @param duration    audio duration
     * @param format      audio track format
     * @param metaData    metadata, loaded from file, or created by hands
     * @throws NullPointerException if trackData or format is null
     * @throws AudioEngineException if trackData is not a direct buffer
     */
    public Track(AudioSource audioSource, Duration duration, AudioFormat format, TrackMetadata metaData) {
        Objects.requireNonNull(audioSource, "audioSource");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(format, "format");

        this.audioSource = audioSource;
        this.duration = duration;
        this.format = format;
        this.metaData = metaData; // Can be null
    }

    public AudioSource getAudioSource() {
        return audioSource;
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
                '}';
    }
}