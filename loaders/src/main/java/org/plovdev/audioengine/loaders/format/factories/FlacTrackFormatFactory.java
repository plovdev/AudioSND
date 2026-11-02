package org.plovdev.audioengine.loaders.format.factories;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteOrder;

/**
 * Factory of .flac(and simily) formats.
 * Contains different methods to create flac audio.
 *
 * @author Anton
 * @version 1.0
 */
public class FlacTrackFormatFactory {
    private FlacTrackFormatFactory() {}

    public static TrackFormat flac16bitStereo44kHz() {
        return new TrackFormat(
                2,
                16,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.FLAC
        );
    }

    public static TrackFormat flac24bitStereo96kHz() {
        return new TrackFormat(
                2,
                24,
                96000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.FLAC
        );
    }
}