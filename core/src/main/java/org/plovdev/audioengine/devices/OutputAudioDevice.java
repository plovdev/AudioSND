package org.plovdev.audioengine.devices;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Base interface for audio output devices (speakers, headphones).
 * Provides methods for writing audio data to the device.
 *
 * @author Anton
 * @version 1.0
 * @see AudioDevice
 * @see InputAudioDevice
 */
public interface OutputAudioDevice extends AudioDevice {

    /**
     * Writes audio data from the buffer to the output device.
     * Blocks until at least one byte is written or device is closed.
     * The method may write fewer bytes than requested.
     *
     * @param byteBuffer buffer containing audio data to write
     * @return number of bytes actually written, or -1 if device is closed
     * @throws IllegalStateException if device is not opened
     */
    int write(@NotNull ByteBuffer byteBuffer);
}