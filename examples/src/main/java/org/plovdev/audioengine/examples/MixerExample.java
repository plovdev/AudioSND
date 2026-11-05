package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.mixer.NativeTrackMixer;
import org.plovdev.audioengine.mixer.TrackMixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class MixerExample {
    private static final Logger log = LoggerFactory.getLogger(MixerExample.class);

    void main(String[] args) {
        try (AudioEngine engine = new NativeAudioEngine()) {
            Track track1 = engine.loadTrack(new File("testdata/wav/48000/16/White Night.wav"));
            Track track2 = engine.loadTrack(new File("testdata/Cotton Eye joe.wav"));

            TrackMixer mixer = new NativeTrackMixer();
            mixer.setOutputFormat(track2.getFormat());
            mixer.addTrack(track1);
            mixer.addTrack(track2);

            Track track = mixer.doMixing();
            engine.createTrackPlayer(track).play();
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("AudioEngine error: ", e);
        }
    }
}