package org.plovdev.audioengine.devices;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.exceptions.devices.OpenAudioDeviceException;
import org.plovdev.audioengine.format.AudioFormat;

/**
 * Base interface for all audio devices (input or output).
 * Most abstractly device description.
 * <p>
 * Devices follow this lifecycle:
 * 1. {@link #open(AudioFormat)} - allocate resources
 * 2. {@link #close()} - release resources
 * </p>
 *
 * @author Anton
 * @version 1.0
 */
public interface AudioDevice extends AutoCloseable {

    /**
     * Open audio device with specified format.
     *
     * @param format working format
     * @throws OpenAudioDeviceException when opening failed.
     */
    void open(@NotNull AudioFormat format) throws OpenAudioDeviceException;

    /**
     * Checks if this format is supported by the audio device.
     *
     * @param format checking format.
     * @return is supported
     */
    default boolean isSupportedFormat(@NotNull AudioFormat format) {
        return getDeviceInfo().supportedFormats().stream().anyMatch(tf -> format.channels() == tf.channels() && format.sampleRate() == tf.sampleRate());
    }

    /**
     * Return all info about audio device
     *
     * @return device info
     */
    AudioDeviceInfo getDeviceInfo();

    /**
     * Return status of audio device, which as opened, closed, etc.
     *
     * @return device status
     */
    AudioDeviceStatus getDeviceStatus();

    /**
     * Checks if the audio device is open.
     *
     * @return is audio device open.
     */
    boolean isOpen();

    @Override
    void close();
}