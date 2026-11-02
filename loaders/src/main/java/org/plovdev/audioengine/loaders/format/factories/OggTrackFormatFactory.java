package org.plovdev.audioengine.loaders.format.factories;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteOrder;

/**
 * Factory of .ogg(and simily) formats.
 * Contains different methods to create ogg audio.
 *
 * @author Anton
 * @version 1.0
 */
public class OggTrackFormatFactory {
    private OggTrackFormatFactory() {}

    public static TrackFormat oggVorbisStereo44kHz() {
        return new TrackFormat(
                2,
                0,
                44100,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.VORBIS
        );
    }

    public static TrackFormat oggOpusStereo48kHz() {
        return new TrackFormat(
                2,
                0,
                48000,
                true,
                ByteOrder.LITTLE_ENDIAN,
                TrackFormat.AudioCodec.OPUS
        );
    }
}