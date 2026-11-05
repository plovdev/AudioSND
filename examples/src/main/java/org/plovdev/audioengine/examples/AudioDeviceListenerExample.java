package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.devices.AudioDeviceListener;
import org.plovdev.audioengine.devices.AudioDeviceManager;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.api.NativeTrackPlayer;
import org.plovdev.audioengine.api.source.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioDeviceListenerExample {
    private static final Logger log = LoggerFactory.getLogger(AudioDeviceListenerExample.class);
    private static final ExecutorService service = Executors.newSingleThreadExecutor();

    void main() {
        try (AudioEngine engine = new NativeAudioEngine()) {
            Track track = engine.loadTrack(new File("testdata/wav/48000/24/block-story-stereo.wav"));
            AudioDeviceManager manager = AudioDeviceManager.getInstance();
            AudioDeviceInfo defaultOut = manager.getDefaultOutputAudioDevice();

            try (TrackPlayer player = new NativeTrackPlayer(track, defaultOut)) {
                player.play();

                manager.addAudioDeviceListener(new AudioDeviceListener() {
                    @Override
                    public void onDeviceConnected(AudioDeviceInfo info) {
                        if (info.type() != AudioDeviceInfo.AudioDeviceType.INPUT) {
                            player.setAudioDevice(info);
                        }
                    }

                    @Override
                    public void onDeviceDisconnected(AudioDeviceInfo info) {
                        if (info != defaultOut) {
                            player.setAudioDevice(defaultOut);
                        }
                    }
                });

                Thread.sleep(track.getDuration());
            }
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }
}