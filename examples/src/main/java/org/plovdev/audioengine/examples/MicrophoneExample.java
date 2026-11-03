package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.NativeAudioRecorder;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.devices.AudioDeviceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrophoneExample {
    private static final Logger log = LoggerFactory.getLogger(MicrophoneExample.class);

    void main(String[] args) {
        try (AudioEngine engine = new NativeAudioEngine();
             NativeAudioRecorder microphone = new NativeAudioRecorder(AudioDeviceManager.getInstance().getDefaultInputAudioDevice().supportedFormats().getFirst().toAudioFormat(), AudioDeviceManager.getInstance().getDefaultInputAudioDevice())) {
            microphone.setOnChunkRecorded(() -> System.out.println("Chunk played"));
            microphone.start(); // начинаем запись
            Thread.sleep(10000); // условно, ждем сколько надо.
            Track track = microphone.stop(); // получаем результат, и делаем что надо.

            engine.createTrackPlayer(track).play();
            Thread.sleep(track.getDuration());
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }
}