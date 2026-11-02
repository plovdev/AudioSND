package org.plovdev.audioengine.loaders.format.factories;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteOrder;

/**
 * Factory of .aiff(and simily) formats.
 * Contains different methods to create aiff audio.
 *
 * @author Anton
 * @version 1.0
 */
public class AiffTrackFormatFactory {
    public static TrackFormat aiff16bitStereo44kHz() {
        return new TrackFormat(
                2,
                16,
                44100,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.PCM16
        );
    }

    public static TrackFormat aiff24bitStereo48kHz() {
        return new TrackFormat(
                2,
                24,
                48000,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.PCM24
        );
    }
}