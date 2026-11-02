package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.*;
import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.generator.TrackGenerator;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.generator.note.Note;
import org.plovdev.audioengine.generator.strategies.envelope.*;
import org.plovdev.audioengine.generator.strategies.frequency.*;
import org.plovdev.audioengine.generator.strategies.noise.*;
import org.plovdev.audioengine.generator.strategies.wave.*;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.format.factories.WavTrackFormatFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class GenerationExample {
    private static final Logger log = LoggerFactory.getLogger(GenerationExample.class);
    private static final AudioEngine engine = new NativeAudioEngine();
    private static final TrackFormat format = WavTrackFormatFactory.wav24bitStereo48kHz();
    private static final Duration duration = Duration.ofSeconds(6);

    void main() {
        Note note = Note.C4;
        GenerationConfig config = GenerationConfig.builder()
                .frequencyStrategy(new ConstantFrequency(2, note.frequency()))
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