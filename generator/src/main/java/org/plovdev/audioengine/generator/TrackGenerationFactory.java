package org.plovdev.audioengine.generator;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.generator.note.Note;
import org.plovdev.audioengine.generator.strategies.envelope.*;
import org.plovdev.audioengine.generator.strategies.frequency.ConstantFrequency;
import org.plovdev.audioengine.generator.strategies.frequency.LinearFrequency;
import org.plovdev.audioengine.generator.strategies.modulation.LFOModulation;
import org.plovdev.audioengine.generator.strategies.noise.BrownNoise;
import org.plovdev.audioengine.generator.strategies.noise.NoiseStrategy;
import org.plovdev.audioengine.generator.strategies.noise.PinkNoise;
import org.plovdev.audioengine.generator.strategies.noise.WhiteNoise;
import org.plovdev.audioengine.generator.strategies.wave.SawtoothWave;
import org.plovdev.audioengine.generator.strategies.wave.SineWave;
import org.plovdev.audioengine.generator.strategies.wave.SquareWave;
import org.plovdev.audioengine.generator.strategies.wave.TriangleWave;

import java.time.Duration;

/**
 * Factory for creating common audio generator configurations.
 *
 * @author Anton
 * @version 1.0
 */
public class TrackGenerationFactory {

    // ============ BASIC WAVES ============

    /**
     * Creates a sine wave tone from a note.
     */
    public static Track generateSineWave(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a square wave tone from a note.
     */
    public static Track generateSquareWave(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float dutyCycle) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SquareWave(0.5f))
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a sawtooth wave tone from a note.
     */
    public static Track generateSawtoothWave(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SawtoothWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a triangle wave tone from a note.
     */
    public static Track generateTriangleWave(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new TriangleWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ NOISE ============

    /**
     * Creates white noise.
     */
    public static Track generateWhiteNoise(Duration duration, @NonNull AudioFormat format) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), 0))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(0.5f))
                .noiseStrategy(new WhiteNoise())
                .noiseLevel(1.0f)
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates pink noise.
     */
    public static Track generatePinkNoise(Duration duration, @NonNull AudioFormat format) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), 0))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(0.5f))
                .noiseStrategy(new PinkNoise())
                .noiseLevel(1.0f)
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates brown noise.
     */
    public static Track generateBrownNoise(Duration duration, @NonNull AudioFormat format) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), 0))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(1f))
                .noiseStrategy(new BrownNoise())
                .noiseLevel(1.0f)
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ FREQUENCY SWEEPS ============

    /**
     * Creates a linear frequency sweep.
     */
    public static Track generateLinearSweep(Duration duration, @NonNull AudioFormat format, @NonNull Note start, @NonNull Note end) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new LinearFrequency(format.channels(), start.frequency(), end.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope((start.amplitude()) + end.amplitude() / 2f))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a linear frequency sweep with square wave.
     */
    public static Track generateSquareSweep(Duration duration, @NonNull AudioFormat format, @NonNull Note start, @NonNull Note end) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new LinearFrequency(format.channels(), start.frequency(), end.frequency()))
                .waveStrategy(new SquareWave(0.5f))
                .envelopeStrategy(new ConstantEnvelope((start.amplitude()) + end.amplitude() / 2f))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ WITH ENVELOPES ============

    /**
     * Creates a tone with ADSR envelope.
     */
    public static Track generateWithADSR(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float attack, float decay, float sustain, float release) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ADSRStrategy(attack, decay, sustain, release))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a tone with linear fade in/out.
     */
    public static Track generateWithFade(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float fadeIn, float fadeOut) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new LinearEnvelope(fadeIn, fadeOut))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates an exponentially decaying tone.
     */
    public static Track generateExponentialDecay(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float decayFactor) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ExponentialEnvelope(decayFactor))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a tone with Hann window envelope.
     */
    public static Track generateHannWindow(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new HannEnvelope())
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ WITH MODULATION ============

    /**
     * Creates a tone with vibrato (frequency modulation).
     */
    public static Track generateWithVibrato(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float rate, float depth) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .modulationStrategy(new LFOModulation(rate, depth, format.sampleRate()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ WITH PAN ============

    /**
     * Creates a tone with stereo panning.
     */
    public static Track generateWithPan(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float pan) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .pan(pan)
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ COMBINED ============

    /**
     * Creates a complex sound with sawtooth wave, ADSR envelope and vibrato.
     */
    public static Track generateComplexTone(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float attack, float decay, float sustain, float release, float vibratoRate, float vibratoDepth) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SawtoothWave())
                .envelopeStrategy(new ADSRStrategy(attack, decay, sustain, release))
                .modulationStrategy(new LFOModulation(vibratoRate, vibratoDepth, format.sampleRate()))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a tone with noise mixed in.
     */
    public static Track generateWithNoise(Duration duration, @NonNull AudioFormat format, @NonNull Note note, float noiseLevel, NoiseStrategy noiseStrategy) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .noiseStrategy(noiseStrategy)
                .noiseLevel(noiseLevel)
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Creates a silent track.
     */
    public static Track generateSilence(Duration duration, @NonNull AudioFormat format) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), 0))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ConstantEnvelope(0))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    // ============ INSTRUMENT PRESETS ============

    /**
     * Simple piano-like sound.
     */
    public static Track generatePianoLike(Duration duration, AudioFormat format, Note note) {
        return generateWithADSR(duration, format, note, 0.09f, 0.1f, 0.1f, 0.1f);
    }

    /**
     * Simple guitar-like sound.
     */
    public static Track generateGuitarLike(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SquareWave(0.5f))
                .envelopeStrategy(new ADSRStrategy(0.5f, 0.5f, 0.8f, 0.1f))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }

    /**
     * Simple flute-like sound.
     */
    public static Track generateFluteLike(Duration duration, @NonNull AudioFormat format, @NonNull Note note) {
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .waveStrategy(new SineWave())
                .envelopeStrategy(new ADSRStrategy(0.1f, 0.1f, 0.8f, 0.2f))
                .build();

        return new TrackGenerator(config, format).generate(duration);
    }
}