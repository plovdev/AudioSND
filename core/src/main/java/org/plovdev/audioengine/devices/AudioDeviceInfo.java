package org.plovdev.audioengine.devices;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;

import java.util.List;
import java.util.Objects;

/**
 * Describes a specific audio device.
 *
 * @param id device identifier
 * @param name device name
 * @param vendor device manufacturer
 * @param type device type (input, output, or duplex)
 * @param supportedFormats formats supported by the device
 *
 * @see TrackFormat
 *
 * @version 1.0
 * @author Anton
 */
public record AudioDeviceInfo(@NotNull String id, String name, String vendor, @NotNull AudioDeviceType type, @NotNull List<TrackFormat> supportedFormats) {
    public AudioDeviceInfo {
        supportedFormats = List.copyOf(supportedFormats);
    }

    public enum AudioDeviceType {
        /** Input device (microphone, line-in) */
        INPUT,

        /** Output device (speakers, headphones) */
        OUTPUT,

        /** Full-duplex device (input and output) */
        DUPLEX
    }

    @Override
    public String toString() {
        return String.format("%s(%s) powered by %s. AudioDevice type: %s, Supports formats size: %s", name(), id(), vendor().replaceAll("\\.$", ""), type.name(), supportedFormats().size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AudioDeviceInfo that = (AudioDeviceInfo) o;
        return Objects.equals(id, that.id); // ID is unique
    }
}