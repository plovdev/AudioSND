package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.loaders.format.factories.WavTrackFormatFactory;
import org.plovdev.audioengine.generator.TrackGenerationFactory;
import org.plovdev.audioengine.generator.note.Note;
import org.plovdev.audioengine.loaders.io.TrackOutputStream;
import org.plovdev.audioengine.loaders.wav.WavTrackOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.time.Duration;

public class TrackOutputStreamExample {
    private static final Logger log = LoggerFactory.getLogger(TrackOutputStreamExample.class);

    static void main() {
        try (AudioEngine engine = new NativeAudioEngine();
             TrackOutputStream stream = new WavTrackOutputStream(WavTrackFormatFactory.wav16bitStereo44kHz(), new FileOutputStream("testwrited.wav"))) {
            stream.writeHead();
            stream.write(TrackGenerationFactory.generateSineWave(Duration.ofSeconds(5), WavTrackFormatFactory.wav16bitStereo44kHz(), Note.A4).getTrackData());
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }
}