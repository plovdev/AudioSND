package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.effects.*;
import org.plovdev.audioengine.format.TrackFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class PlayerExample {
    private static final Logger log = LoggerFactory.getLogger(PlayerExample.class);

    void main() {
        try (AudioEngine engine = new NativeAudioEngine()) {
            Track track = engine.loadTrack(new File("testdata/Cotton Eye Joe.wav"));
            TrackFormat format = track.getFormat();

            EffectsChain chain = new EffectsChain();
            chain.addEffect(new ReverbEffect(format));
            track = chain.apply(track);

            TrackPlayer player = engine.createTrackPlayer(track);
            player.play();
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }
}