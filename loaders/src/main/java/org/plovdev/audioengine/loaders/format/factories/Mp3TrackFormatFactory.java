package org.plovdev.audioengine.loaders.format.factories;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteOrder;

/**
 * Factory of .mp3(and simily) formats.
 * Contains different methods to create mp3 audio.
 *
 * @author Anton
 * @version 1.0
 */
public class Mp3TrackFormatFactory {
    private Mp3TrackFormatFactory() {}

    public static TrackFormat mp3Stereo64kbps() {
        return new TrackFormat(
                2,
                0,     // compressed
                44100,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.MP3
        );
    }

    public static TrackFormat mp3Stereo128kbps() {
        return new TrackFormat(
                2,
                0,
                44100,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.MP3
        );
    }

    public static TrackFormat mp3Stereo192kbps() {
        return new TrackFormat(
                2,
                0,
                44100,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.MP3
        );
    }

    public static TrackFormat mp3Stereo320kbps() {
        return new TrackFormat(
                2,
                0,
                44100,
                true,
                ByteOrder.BIG_ENDIAN,
                TrackFormat.AudioCodec.MP3
        );
    }
}