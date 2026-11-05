package org.plovdev.audioengine.api;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.devices.AudioDeviceInfo;

import java.time.Duration;

/**
 * Controls playback of a single audio track.
 * <p>
 * Player follows strict lifecycle:
 * <ol>
 *   <li>{@link #play()} - start playback</li>
 *   <li>{@link #pause()} - pause playback</li>
 *   <li>{@link #stop()} - stop playback</li>
 *   <li>{@link #close()} - release all resources</li>
 * </ol>
 * </p>
 * <p>
 * Additional controls:
 * <ul>
 *   <li>{@link #setVolume(float)} - adjust volume in real-time</li>
 *   <li>{@link #setSpeed(float)} - change playback speed</li>
 *   <li>{@link #setLoopCount(int)} - configure looping</li>
 *   <li>{@link #seek(Duration)} - seek to position</li>
 *   <li>{@link #setAudioDevice(AudioDeviceInfo)} - switch output device on the fly</li>
 * </ul>
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see Track
 * @see AudioRecorder
 */
public interface TrackPlayer extends AudioController {

    /**
     * Starts or resumes playback from current position.
     * If player is stopped, starts from beginning.
     * If player is paused, resumes from paused position.
     *
     * @throws IllegalStateException if player is not initialized
     */
    void play();

    /**
     * Pauses playback at current position.
     * Playback can be resumed with {@link #play()}.
     *
     * @throws IllegalStateException if player is not playing
     */
    void pause();

    /**
     * Stops playback and resets position to beginning.
     * Player remains initialized and can be played again.
     *
     * @throws IllegalStateException if player is not active
     */
    void stop();

    /**
     * Gets current playback volume.
     *
     * @return volume value (typically 0.0 = silent, 1.0 = normal, >1.0 = boost)
     */
    float getVolume();

    /**
     * Sets playback volume in real-time.
     *
     * @param volume new volume.
     */
    void setVolume(float volume);

    /**
     * Gets current playback speed.
     *
     * @return speed multiplier (1.0 = normal)
     */
    default float getSpeed() {
        throw new UnsupportedOperationException("Not impl. yet");
    }

    /**
     * Sets playback speed multiplier.
     * <p>
     * Note: Speed change may affect pitch unless advanced resampling is used.
     * For pitch-preserving speed change, use a dedicated effect.
     * </p>
     *
     * @param speed speed (0.5 = half, 1.0 = normal, 2.0 = double)
     * @throws IllegalArgumentException      if speed <= 0
     * @throws UnsupportedOperationException if speed change not supported by implementation
     */
    default void setSpeed(float speed) {
        throw new UnsupportedOperationException("Not impl. yet");
    }

    /**
     * Gets loop (repeat) count.
     *
     * @return number of times to repeat (0 = no loop, -1 = infinite)
     */
    int getLoopCount();

    /**
     * Sets loop (repeat) count.
     *
     * @param count number of times to repeat (0 = no loop, -1 = infinite)
     * @throws IllegalArgumentException if count < -1
     */
    void setLoopCount(int count);

    /**
     * Gets current playing cycle number.
     *
     * @return current cycle index.
     */
    int getCurrentCycle();

    /**
     * Gets current playback position.
     *
     * @return elapsed time from start of track
     */
    Duration getCurrentTime();

    /**
     * Seeks to specific position in track.
     * Position is clamped to track duration.
     *
     * @param position target position to seek to
     * @throws IllegalStateException if player is not initialized
     */
    void seek(Duration position);

    /**
     * Switches audio output device during playback.
     * Player will pause briefly during device switch and resume automatically.
     *
     * @param newOutDevice new output device info
     * @throws NullPointerException if device is null
     */
    void setAudioDevice(AudioDeviceInfo newOutDevice);
}