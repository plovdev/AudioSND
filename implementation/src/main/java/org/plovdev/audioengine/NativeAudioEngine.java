package org.plovdev.audioengine;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.NativeTrackPlayer;
import org.plovdev.audioengine.api.TrackPlayer;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.devices.AudioDeviceManager;
import org.plovdev.audioengine.engineconfig.AudioEngineConfig;
import org.plovdev.audioengine.exceptions.AudioEngineException;
import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackExporter;
import org.plovdev.audioengine.loaders.TrackLoader;
import org.plovdev.audioengine.loaders.TrackLoaderManager;
import org.plovdev.audioengine.mixer.TrackMixer;
import org.plovdev.audioengine.utils.NativeLibraryUnpacker;
import org.plovdev.audioengine.utils.TrackLoaderSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
public class NativeAudioEngine implements AudioEngine {
    private static final Logger log = LoggerFactory.getLogger(NativeAudioEngine.class);
    private static final Logger infoLogger = LoggerFactory.getLogger("InfoLogger");
    private static final AtomicBoolean IS_NATIVE_LOADED = new AtomicBoolean(false);

    private final List<TrackLoaderManager> loaderManagers = new CopyOnWriteArrayList<>();
    private AudioEngineConfig config = AudioEngineConfig.load();
    private final AtomicBoolean isInited = new AtomicBoolean(false);

    public NativeAudioEngine() {
        try {
            init(config);
        } catch (AudioEngineException e) {
            throw new AudioEngineException(e);
        }
    }

    public NativeAudioEngine(AudioEngineConfig config) {
        this.config = config;
        try {
            init(config);
        } catch (AudioEngineException e) {
            throw new AudioEngineException(e);
        }
    }

    /**
     * Initializes engine with custom configuration.
     *
     * @param config engine configuration
     * @throws IllegalStateException if already initialized
     * @throws AudioEngineException  if initialization fails
     */
    @Override
    public synchronized void init(@NotNull AudioEngineConfig config) throws AudioEngineException {
        if (!isInited.get()) {
            try {
                this.config = config;
                printEngineInfo(config);

                initNative();
                TrackLoaderSearcher.searchAvailableTrackLoaderManagers().forEach(this::addLoaderManager);
                isInited.set(true);
            } catch (Exception e) {
                throw new AudioEngineException("Cann't init AudioSND: " + e.getMessage());
            }
        } else throw new AudioEngineException("Engine is already inited!");
    }

    private void printEngineInfo(AudioEngineConfig config) {
        Runtime runtime = Runtime.getRuntime();

        String banner = String.format("\033[32;1m\n%s\033[0m", config.getBanner());
        System.out.println(banner);

        float bytesDev = 1024.0f * 1024.0f;
        infoLogger.info("Using configuration: {}", config);
        infoLogger.info("System statistics:");
        infoLogger.info("    * Available processors: {}", runtime.availableProcessors());
        infoLogger.info("    * Memory usage: {}/{}MB", (runtime.freeMemory() / bytesDev), runtime.maxMemory() / bytesDev);
        infoLogger.info("Native library: {}", config.getNativeLib().toString());
    }

    /**
     * @return true if engine is initialized and ready
     */
    @Override
    public boolean isInitialized() {
        return isInited.get();
    }

    /**
     * @return current engine configuration
     * @throws IllegalStateException if not initialized
     */
    @Override
    public AudioEngineConfig getConfig() {
        return config;
    }


    @Override
    public Track loadTrack(@NotNull File file) throws TrackLoadException {
        checkIfInited();
        for (TrackLoaderManager manager : getAvailableLoaders()) {
            TrackLoader loader = manager.getTrackLoader();
            if (manager.isSupported(file)) {
                return loader.loadTrack(file);
            }
        }
        throw new TrackLoadException("Loader not found fot this track source.");
    }

    @Override
    public Track loadTrack(@NotNull InputStream stream) throws TrackLoadException {
        checkIfInited();
        for (TrackLoaderManager manager : getAvailableLoaders()) {
            TrackLoader loader = manager.getTrackLoader();
            if (manager.isSupported(stream)) {
                return loader.loadTrack(stream);
            }
        }
        throw new TrackLoadException("Loader not found fot this track source(input stream).");
    }

    @Override
    public TrackPlayer createTrackPlayer(@NotNull Track track) {
        checkIfInited();
        return new NativeTrackPlayer(track, AudioDeviceManager.getInstance().getDefaultOutputAudioDevice());
    }

    @Override
    public void addLoaderManager(@NotNull TrackLoaderManager loader) {
        loaderManagers.add(loader);
    }

    @Override
    public void removeLoaderManager(@NotNull TrackLoaderManager loader) {
        loaderManagers.remove(loader);
    }

    @Override
    public List<TrackLoaderManager> getAvailableLoaders() {
        return loaderManagers;
    }

    @Override
    public List<AudioDeviceInfo> getAvailableInputAudioDevices() {
        checkIfInited();
        return AudioDeviceManager.getInstance().getInputAudioDevices();
    }

    @Override
    public List<AudioDeviceInfo> getAvailableOutputAudioDevices() {
        checkIfInited();
        return AudioDeviceManager.getInstance().getOutputAudioDevices();
    }

    /**
     * Finds suitable loader for given format.
     *
     * @param format givven format
     * @return loader or empty Optional if none can handle
     */
    @Override
    public Optional<TrackLoaderManager> findLoaderFor(@NotNull TrackFormat format) {
        for (TrackLoaderManager manager : loaderManagers) {
            if (manager.isSupported(format)) {
                return Optional.of(manager);
            }
        }
        return Optional.empty();
    }

    @Override
    public synchronized void close() {
        if (!isInited.get()) {
            return;
        }
        loaderManagers.clear();
        _cleanup();
        isInited.set(false);
    }

    private void checkIfInited() {
        if (!isInited.get()) {
            throw new AudioEngineException("AudioSND hasn't been inited!");
        }
    }

    @Override
    public Optional<TrackLoaderManager> getTrackLoaderManager(Class<? extends TrackLoaderManager> loader) {
        for (TrackLoaderManager manager : getAvailableLoaders()) {
            if (manager.getClass().isAssignableFrom(loader)) {
                return Optional.of(manager);
            }
        }
        return Optional.empty();
    }

    @Override
    public void exportTrack(@NonNull Track track, TrackFormat outputFromat, OutputStream outputStream) {
        Optional<TrackLoaderManager> exporterOptional = findLoaderFor(outputFromat);

        if (exporterOptional.isPresent()) {
            TrackLoaderManager trackLoaderManager = exporterOptional.get();
            TrackExporter exporter = trackLoaderManager.getTrackExporter();
            exporter.save(track, outputFromat, outputStream);
        } else {
            throw new TrackExportException("Cann't find situable exporter");
        }
    }

    private synchronized void initNative() {
        if (!IS_NATIVE_LOADED.get()) {
            try {
                NativeLibraryUnpacker unpacker = new NativeLibraryUnpacker(config.getNativeLib());
                System.load(unpacker.unpackLib());
                _init();
                IS_NATIVE_LOADED.set(true);
            } catch (Throwable e) {
                throw new AudioEngineException(e);
            }
        }
    }

    //====== Natives ======\\
    private native void _init();

    private native void _cleanup();
}