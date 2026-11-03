package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.format.AudioFormatFactory;
import org.plovdev.audioengine.generator.TrackGenerator;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.generator.note.Note;
import org.plovdev.audioengine.generator.strategies.envelope.ConstantEnvelope;
import org.plovdev.audioengine.generator.strategies.frequency.ConstantFrequency;
import org.plovdev.audioengine.generator.strategies.noise.PinkNoise;
import org.plovdev.audioengine.generator.strategies.wave.TriangleWave;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class GenerationExample {
    private static final Logger log = LoggerFactory.getLogger(GenerationExample.class);
    private static final AudioEngine engine = new NativeAudioEngine();
    private static final AudioFormat format = AudioFormatFactory.stereo48000();
    private static final Duration duration = Duration.ofSeconds(6);

    void main() {
        Note note = Note.C4;
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(format.channels(), note.frequency()))
                .envelopeStrategy(new ConstantEnvelope(note.amplitude()))
                .waveStrategy(new TriangleWave())
                .noiseStrategy(new PinkNoise())
                .noiseLevel(0.1f)
                .build();
        TrackGenerator generator = new TrackGenerator(config, format);

        Track track = generator.generate(duration);
        play(track);
    }

    private static void play(Track track) {
        try (TrackPlayer player = engine.createTrackPlayer(track)) {
            player.play();
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("Playing error: ", e);
        }
    }
}