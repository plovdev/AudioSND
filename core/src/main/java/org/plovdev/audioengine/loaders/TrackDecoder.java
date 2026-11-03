package org.plovdev.audioengine.loaders;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.format.TrackFormat;

/**
 * Decodes audio tracks into PCM format.
 * <p>
 * Implementations convert compressed or non-PCM audio tracks
 * into the specified output PCM format.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @since 1.0
 */
public interface TrackDecoder {

    /**
     * Decodes the input track to the specified PCM format.
     * <p>
     * Creates a new track instance. Original track is not modified.
     * Output format must be a PCM format supported by the decoder.
     * </p>
     *
     * @param input       source track (compressed or non-PCM)
     * @param inputFormat input format (sample rate, channels, bit depth)
     * @return new track containing decoded PCM audio data
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if decoding fails or output format is not supported
     */
    @NotNull
    Track decode(@NotNull Track input, @NotNull TrackFormat inputFormat);
}