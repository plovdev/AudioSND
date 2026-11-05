package org.plovdev.audioengine.api;

import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.exceptions.devices.OpenAudioDeviceException;
import org.plovdev.audioengine.format.AudioFormat;

/**
 * Base interface for all high-level audio controls.
 * <p>
 * Provides common functionality for both playback and recording.
 * Implementations: {@link TrackPlayer}, {@link AudioRecorder}
 * </p>
 *
 * @author Anton
 * @version 1.0
 */
public interface AudioController extends AutoCloseable {

    /**
     * Initializes the player with track format and opens audio device.
     * Called automatically in constructor, but can be called manually if needed.
     *
     * @throws OpenAudioDeviceException if cann't init device.
     */
    void init();

    /**
     * Returns current audio device being used.
     */
    AudioDeviceInfo getCurrentAudioDevice();

    /**
     * Returns current player status.
     *
     * @return current status (RUNNING, PAUSED, STOPPED, etc.)
     */
    AudioStatus getStatus();

    /**
     * Returns current track format.
     *
     * @return current format.
     */
    AudioFormat getCurrentFormat();

    /**
     * Releases all resources.
     */
    @Override
    void close();
}