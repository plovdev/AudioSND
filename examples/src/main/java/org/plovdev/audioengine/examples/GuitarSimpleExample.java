package org.plovdev.audioengine.examples;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.devices.*;
import org.plovdev.audioengine.effects.OverdriveEffect;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.format.AudioFormatFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuitarSimpleExample {
    private static final Logger log = LoggerFactory.getLogger(GuitarSimpleExample.class);
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);

    static void main(String @NonNull [] args) {
        boolean needWrite = Boolean.parseBoolean(args[0]);
        String fileName = args[1];

        runStopThread();
        AudioFormat format = AudioFormatFactory.stereo48000();

        try (AudioEngine engine = new NativeAudioEngine()) {
            AudioDeviceManager manager = AudioDeviceManager.getInstance();
            AudioDeviceInfo inputInfo = manager.getDefaultInputAudioDevice();
            AudioDeviceInfo outputInfo = manager.getOutputDeviceById("48");

            OverdriveEffect effect = new OverdriveEffect(format, OverdriveEffect.OverdriveType.SOFT_CLIPPING, 0.6f, 0.6f, 0.6f, 0.6f, 0.03f);

            try (InputAudioDevice input = new NativeInputAudioDevice(inputInfo);
                 OutputAudioDevice output = new NativeOutputAudioDevice(outputInfo)) {
                input.open(format);
                output.open(format);

                ByteArrayOutputStream savedTrack = new ByteArrayOutputStream();
                ByteBuffer buffer = ByteBuffer.allocateDirect(512);
                byte[] bytes = new byte[512];

                log.info("Start cycle: isRunning: {}", isRunning.get());
                while (isRunning.get()) {
                    input.read(buffer);
                    effect.processInPlace(buffer);
                    output.write(buffer);
                    buffer.position(0);
                    if (needWrite) {
                        buffer.get(bytes);
                        savedTrack.write(bytes);
                    }
                }

                ByteBuffer writed = ByteBuffer.allocateDirect(savedTrack.size());
                writed.put(savedTrack.toByteArray());
            }
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }

    private static void runStopThread() {
        Thread.startVirtualThread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                String stop = scanner.nextLine();
                if (stop.equals("s")) {
                    isRunning.set(false);
                }
            } catch (Exception e) {
                log.error("Stopping error: ", e);
            }
        });
    }
}