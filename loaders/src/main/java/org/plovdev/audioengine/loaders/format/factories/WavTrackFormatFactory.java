package org.plovdev.audioengine.loaders.format.factories;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteOrder;

/**
 * Factory of .wav(and simily) formats.
 * Contains different methods to create wav audio.
 *
 * @author Anton
 * @version 1.0
 */
public class WavTrackFormatFactory {
    private WavTrackFormatFactory() {}

    public static TrackFormat wav8bitMono44kHz() {
        return new TrackFormat(
                1,
                8,
                44100,
                false,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM8
        );
    }

    public static TrackFormat wav16bitMono44kHz() {
        return new TrackFormat(
                1,
                16,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat wav16bitStereo44kHz() {
        return new TrackFormat(
                2,
                16,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat wav24bitStereo44kHz() {
        return new TrackFormat(
                2,
                24,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }

    public static TrackFormat wav32bitFloatStereo44kHz() {
        return new TrackFormat(
                2,
                32,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.FLOAT32
        );
    }

    public static TrackFormat wav16bitStereo48kHz() {
        return new TrackFormat(
                2,
                16,
                48000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat wav24bitStereo48kHz() {
        return new TrackFormat(
                2,
                24,
                48000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }

    public static TrackFormat wav32bitFloatStereo48kHz() {
        return new TrackFormat(
                2,
                32,
                48000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.FLOAT32
        );
    }

    public static TrackFormat wav32bitFloatStereo96kHz() {
        return new TrackFormat(
                2,
                32,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.FLOAT32
        );
    }

    public static TrackFormat wav16bitStereo96kHz() {
        return new TrackFormat(
                2,
                16,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat wav24bitStereo96kHz() {
        return new TrackFormat(
                2,
                24,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }

    public static TrackFormat wav24bitStereo192kHz() {
        return new TrackFormat(
                2,
                24,
                192000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }

    public static TrackFormat raw16bitStereo44kHz() {
        return new TrackFormat(
                2,
                16,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat raw32bitFloatStereo96kHz() {
        return new TrackFormat(
                2,
                32,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM32
        );
    }

    public static TrackFormat studioMaster24bit96kHz() {
        return new TrackFormat(
                2,
                24,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }

    public static TrackFormat studioMaster32bitFloat192kHz() {
        return new TrackFormat(
                2,
                32,
                192000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.PCM32
        );
    }
}