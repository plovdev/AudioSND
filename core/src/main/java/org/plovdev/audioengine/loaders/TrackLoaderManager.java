package org.plovdev.audioengine.loaders;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.TrackFormat;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * Central management point for track loading and processing.
 * <p>
 * Provides access to:
 * <ul>
 *   <li>{@link TrackLoader} - loads raw audio data</li>
 *   <li>{@link TrackDecoder} - decodes compressed formats to PCM</li>
 *   <li>{@link TrackEncoder} - encodes PCM to compressed formats</li>
 *   <li>{@link TrackExporter} - writes tracks to output streams</li>
 * </ul>
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see Track
 * @see TrackFormat
 * @since 1.0
 */
public interface TrackLoaderManager {
    @NotNull TrackLoader getTrackLoader();

    @NotNull TrackExporter getTrackExporter();

    @NotNull TrackEncoder getTrackEncoder();

    @NotNull TrackDecoder getTrackDecoder();

    /**
     * Checks if this loader supports the given file.
     *
     * @param file file to check
     * @return true if file format is supported
     */
    boolean isSupported(@NotNull File file);

    /**
     * Checks if this loader supports the given audio format.
     * <p>
     * Useful for quick pre-validation without accessing actual file/stream.
     * </p>
     *
     * @param format audio format to check
     * @return true if format is supported
     */
    boolean isSupported(@NotNull TrackFormat format);

    /**
     * Checks if this loader supports the given input stream.
     *
     * @param stream stream to check
     * @return true if stream format is supported
     */
    boolean isSupported(@NotNull InputStream stream);

    /**
     * Checks if this loader supports the given URI.
     *
     * @param uri URI to check
     * @return true if URI protocol and format are supported
     */
    boolean isSupported(@NotNull URI uri);
}