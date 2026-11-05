package org.plovdev.audioengine.generator;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.generator.strategies.envelope.EnvelopeStrategy;
import org.plovdev.audioengine.generator.strategies.frequency.FrequencyStrategy;
import org.plovdev.audioengine.generator.strategies.modulation.ModulationStrategy;
import org.plovdev.audioengine.generator.strategies.noise.NoiseStrategy;
import org.plovdev.audioengine.generator.strategies.wave.WaveStrategy;
import org.plovdev.audioengine.math.AudioMath;
import org.plovdev.audioengine.metadata.TrackMetadata;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        int sampleRate = trackFormat.sampleRate();
        int channels = trackFormat.channels();
        long totalSamples = duration.toMillis() * sampleRate / 1000;
        int bufferSize = (int) (totalSamples * channels * 4);

        ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        FrequencyStrategy freqStrategy = config.getFrequencyStrategy();
        WaveStrategy waveStrategy = config.getWaveStrategy();
        EnvelopeStrategy envStrategy = config.getEnvelopeStrategy();
        ModulationStrategy modulationStrategy = config.getModulationStrategy();
        NoiseStrategy noiseStrategy = config.getNoiseStrategy();

        for (long samplePos = 0; samplePos < totalSamples; samplePos++) {
            float[] channelSamples = new float[channels];

            for (int channel = 0; channel < channels; channel++) {
                float frequency = freqStrategy.getFrequency(channel, samplePos, totalSamples);

                if (modulationStrategy != null) {
                    frequency = modulationStrategy.modulate(samplePos, totalSamples, channel, frequency);
                }
                float sample = waveStrategy.generate(samplePos, frequency, config.getPhase(), sampleRate);
                float amplitude = envStrategy.getAmplitude(samplePos, totalSamples, 1);
                sample *= amplitude;

                if (config.getNoiseLevel() > 0 && noiseStrategy != null) {
                    float noise = noiseStrategy.nextSample(channel);
                    sample = sample * (1 - config.getNoiseLevel()) + noise * config.getNoiseLevel();
                }

                channelSamples[channel] = sample;
            }

            if (channels == 2) {
                float pan = config.getPan();
                float leftGain, rightGain;

                if (pan <= 0) {
                    leftGain = 1.0f;
                    rightGain = 1.0f + pan;
                } else {
                    leftGain = 1.0f - pan;
                    rightGain = 1.0f;
                }

                channelSamples[0] *= leftGain;
                channelSamples[1] *= rightGain;
            }

            for (int channel = 0; channel < channels; channel++) {
                float sample = AudioMath.clamp(channelSamples[channel], -1.0f, 1.0f);
                buffer.putFloat(sample);
            }
        }

        buffer.rewind();
        return new Track(buffer, duration, trackFormat, new TrackMetadata());
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