package org.plovdev.audioengine.generator;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.AudioSource;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.metadata.TrackMetadata;

import java.time.Duration;

/**
 * Audio generator that uses strategies to produce sound.
 * <p>
 * Combines frequency, wave, and envelope strategies to generate audio samples.
 * </p>
 *
 * @author Anton
 * @version 1.0
 */
public class TrackGenerator {
    private GenerationConfig config;
    private final AudioFormat trackFormat;

    public TrackGenerator(GenerationConfig config, AudioFormat format) {
        this.config = config;
        trackFormat = format;
    }

    /**
     * Generates audio track of specified duration.
     *
     * @param duration track duration
     * @return generated Track
     */
    public synchronized Track generate(@NonNull Duration duration) {
        return new Track(new AudioSource(new TrackGeneratorProducer(duration, trackFormat, config)), duration, trackFormat, TrackMetadata.DEFAULT_METADATA);
    }

    public GenerationConfig getConfig() {
        return config;
    }

    public synchronized void setConfig(GenerationConfig config) {
        this.config = config;
    }

    public AudioFormat getTrackFormat() {
        return trackFormat;
    }
}