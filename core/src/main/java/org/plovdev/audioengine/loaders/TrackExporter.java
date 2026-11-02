package org.plovdev.audioengine.loaders;

import org.plovdev.audioengine.api.Track;
import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;

import java.io.OutputStream;

/**
 * Exports audio tracks to an output stream.
 * <p>
 * Implementations write track audio data in a specific format
 * (WAV, MP3, FLAC, etc.) to the provided output stream.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @since 1.0
 */
public interface TrackExporter {

    /**
     * Writes the track audio data to the output stream.
     * <p>
     * The output format is implementation-specific and should be
     * documented by concrete implementations.
     * Original track is not modified.
     * </p>
     *
     * @param track        source track to export
     * @param outputStream destination stream (not closed by this method)
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackExportException
     *         if export operation fails
     * @throws NullPointerException if any argument is null
     */
    void save(@NotNull Track track, TrackFormat toFormat, @NotNull OutputStream outputStream);
}