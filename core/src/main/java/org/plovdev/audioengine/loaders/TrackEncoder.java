package org.plovdev.audioengine.loaders;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.TrackFormat;
import org.jetbrains.annotations.NotNull;

/**
 * Encodes PCM audio tracks into compressed or non-PCM formats.
 * <p>
 * Implementations convert raw PCM audio tracks into the specified
 * output format (MP3, AAC, FLAC, WAV, etc.).
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @since 1.0
 */
public interface TrackEncoder {

    /**
     * Encodes the input PCM track to the specified output format.
     * <p>
     * Creates a new track instance. Original track is not modified.
     * Input track must be in PCM format.
     * </p>
     *
     * @param input     source track (PCM audio)
     * @param outFormat target format (e.g., MP3, AAC, FLAC)
     * @return new track containing encoded audio data
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException
     *         if encoding fails, input is not PCM, or output format is not supported
     */
    @NotNull
    Track encode(@NotNull Track input, @NotNull TrackFormat outFormat);
}