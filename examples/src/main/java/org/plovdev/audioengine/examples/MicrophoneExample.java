package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.api.NativeAudioRecorder;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.devices.AudioDeviceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MicrophoneExample {
    private static final Logger log = LoggerFactory.getLogger(MicrophoneExample.class);

    void main(String[] args) {
        try (AudioEngine engine = new NativeAudioEngine();
             NativeAudioRecorder microphone = new NativeAudioRecorder(AudioDeviceManager.getInstance().getDefaultInputAudioDevice().supportedFormats().getFirst().toAudioFormat(), AudioDeviceManager.getInstance().getDefaultInputAudioDevice())) {
            microphone.setOnChunkRecorded(() -> System.out.println("Chunk played"));
            Track track = microphone.start(); // начинаем запись
            engine.createTrackPlayer(track).play();

            Thread.sleep(10000); // условно, ждем сколько надо.
            microphone.stop(); // получаем результат, и делаем что надо.
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }
}