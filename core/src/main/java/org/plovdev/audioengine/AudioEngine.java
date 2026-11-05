package org.plovdev.audioengine;

import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.engineconfig.AudioEngineConfig;
import org.plovdev.audioengine.exceptions.AudioEngineException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackLoaderManager;
import org.plovdev.audioengine.mixer.TrackMixer;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Main entry point for AudioSND.
 * <p>
 * Provides audio loading, mixing, and playback capabilities.
 * Engine must be {@link #init(AudioEngineConfig)} before use and {@link #close()} after.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see Track
 * @see TrackPlayer
 * @see TrackMixer
 */
public interface AudioEngine extends AutoCloseable {
    /**
     * Initializes engine with custom configuration.
     *
     * @param config engine configuration
     * @throws IllegalStateException if already initialized
     * @throws AudioEngineException  if initialization fails
     */
    void init(AudioEngineConfig config);

    /**
     * Returns true if engine is initialized and ready.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Returns current engine configuration.
     *
     * @return engine configuration
     * @throws IllegalStateException if not initialized
     */
    AudioEngineConfig getConfig();

    /**
     * Loads and decodes audio track from a file.
     *
     * @param file source file
     * @return decoded track
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails
     */
    Track loadTrack(File file);

    /**
     * Loads and decodes audio track from an input stream.
     *
     * @param stream source stream
     * @return decoded track
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails
     */
    Track loadTrack(InputStream stream);

    /**
     * Loads and decodes audio track from a URI.
     *
     * @param uri source URI
     * @return decoded track
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackLoadException if loading fails
     */
    Track loadTrack(URI uri);

    /**
     * Creates a new player instance for the specified track.
     * <p>
     * Caller is responsible for closing the player when no longer needed.
     * </p>
     *
     * @param track track to play
     * @return new track player
     */
    TrackPlayer createTrackPlayer(Track track);

    /**
     * Registers a loader manager.
     *
     * @param loader loader manager to add
     */
    void addLoaderManager(TrackLoaderManager loader);

    /**
     * Unregisters a loader manager.
     *
     * @param loader loader manager to remove
     */
    void removeLoaderManager(TrackLoaderManager loader);

    /**
     * Returns all registered loader managers.
     *
     * @return list of available loader managers
     */
    List<TrackLoaderManager> getAvailableLoaders();

    /**
     * Returns all available input audio devices.
     *
     * @return list of input device descriptions
     */
    List<AudioDeviceInfo> getAvailableInputAudioDevices();

    /**
     * Returns all available output audio devices.
     *
     * @return list of output device descriptions
     */
    List<AudioDeviceInfo> getAvailableOutputAudioDevices();

    /**
     * Finds a loader manager that supports the given format.
     *
     * @param format audio format
     * @return loader manager or empty Optional if none found
     */
    Optional<TrackLoaderManager> findLoaderFor(TrackFormat format);

    /**
     * Returns a loader manager of the specified type.
     *
     * @param loader loader manager class
     * @return loader manager or empty Optional if not registered
     */
    Optional<TrackLoaderManager> getTrackLoaderManager(Class<? extends TrackLoaderManager> loader);

    /**
     * Exports track to output stream using a suitable exporter.
     * <p>
     * Selects an exporter that supports the track's audio format.
     * If track is in PCM format, an encoder may be used to compress it.
     * </p>
     *
     * @param track        track to export
     * @param outputStream destination stream
     * @throws org.plovdev.audioengine.exceptions.loaders.TrackExportException if no suitable exporter found or export operation fails
     */
    void exportTrack(Track track, TrackFormat outputFormat, OutputStream outputStream);

    /**
     * Releases all native resources and shuts down the engine.
     * <p>
     * Engine cannot be reused after closing.
     * </p>
     */
    @Override
    void close();
}