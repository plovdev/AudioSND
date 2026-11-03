package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.format.AudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class PlayerExample {
    private static final Logger log = LoggerFactory.getLogger(PlayerExample.class);

    void main() {
        try (AudioEngine engine = new NativeAudioEngine()) {
            Track track = engine.loadTrack(new File("block-story-sf.wav"));
            AudioFormat format = track.getFormat();

            TrackPlayer player = engine.createTrackPlayer(track);
            player.play();
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }
}