package org.plovdev.audioengine.loaders;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.metadata.TrackMetadata;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

/**
 * Loads audio tracks from various sources.
 * <p>
 * Responsible for reading raw audio data and providing access to:
 * <ul>
 *   <li>Full track metadata (tags, artwork, etc.)</li>
 *   <li>Technical audio format information</li>
 * </ul>
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @since 1.0
 */
public interface TrackLoader {

    /**
     * Loads audio track from a file.
     *
     * @param file source file
     * @return track ready to playback or decoding
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails or format is not supported
     */
    @NotNull
    Track loadTrack(@NotNull File file);

    /**
     * Loads audio track from an input stream.
     * <p>
     * Stream is consumed but not closed by this method.
     * </p>
     *
     * @param stream source data stream
     * @return track ready for processing
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails or format is not supported
     */
    @NotNull
    Track loadTrack(@NotNull InputStream stream);

    /**
     * Loads and decodes audio track from a URI.
     * <p>
     * Supports file://, http://, https:// and other protocols
     * depending on implementation.
     * </p>
     *
     * @param uri source URI
     * @return track ready processing
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails or format is not supported
     */
    @NotNull
    Track loadTrack(@NotNull URI uri);

    /**
     * Reads complete metadata from an audio file.
     *
     * @param src source file
     * @return metadata including technical info, tags and artwork
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if metadata cannot be read
     */
    @NotNull
    TrackMetadata readTrackMetadata(@NotNull File src);

    /**
     * Reads complete metadata from an audio stream.
     *
     * @param src source stream
     * @return metadata including technical info, tags and artwork
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if metadata cannot be read
     */
    @NotNull
    TrackMetadata readTrackMetadata(@NotNull InputStream src);

    /**
     * Reads complete metadata from a URI.
     *
     * @param src source URI
     * @return metadata including technical info, tags and artwork
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if metadata cannot be read
     */
    @NotNull
    TrackMetadata readTrackMetadata(@NotNull URI src);

    /**
     * Extracts only technical audio format from a file.
     * <p>
     * Faster than full metadata read when only format information is needed.
     * </p>
     *
     * @param src source file
     * @return audio format (sample rate, channels, bit depth, etc.)
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if format cannot be determined
     */
    @NotNull
    TrackFormat getTrackFormat(@NotNull File src);

    /**
     * Extracts only technical audio format from a stream.
     *
     * @param src source stream
     * @return audio format (sample rate, channels, bit depth, etc.)
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if format cannot be determined
     */
    @NotNull
    TrackFormat getTrackFormat(@NotNull InputStream src);

    /**
     * Extracts only technical audio format from a URI.
     *
     * @param src source URI
     * @return audio format (sample rate, channels, bit depth, etc.)
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if format cannot be determined
     */
    @NotNull
    TrackFormat getTrackFormat(@NotNull URI src);

    /**
     * Sets global listener for all load operations.
     * <p>
     * Note: Listener is shared between concurrent operations.
     * Consider passing listener directly to {@link #loadTrack(File)}
     * if per-operation tracking is needed.
     * </p>
     *
     * @param listener progress listener, may be null to disable
     */
    void setLoadListener(@Nullable LoadListener listener);

    /**
     * Returns currently set global load listener.
     *
     * @return current listener or null if not set
     */
    @Nullable
    LoadListener getLoadListener();
}