package org.plovdev.audioengine.api;

import org.plovdev.audioengine.api.source.Track;

/**
 * Controls audio recording from input device.
 * <p>
 * Recorder follows similar lifecycle to player:
 * <ol>
 *   <li>{@link #start()} - begin recording</li>
 *   <li>{@link #pause()} - temporarily pause</li>
 *   <li>{@link #stop()} - stop and get recorded track</li>
 *   <li>{@link #close()} - release resources</li>
 * </ol>
 * </p>
 *
 * @author Anton
 * @version 1.0
 */
public interface AudioRecorder extends AudioController {
    Track start();

    /**
     * Pauses recording at current position.
     * Recording can be resumed with {@link #start()}.
     *
     * @throws IllegalStateException if not recording
     */
    void pause();

    void stop();

    /**
     * Sets input gain (amplification) for recording.
     *
     * @param gain multiplier (1.0 = normal, >1.0 = boost, <1.0 = attenuate)
     * @throws IllegalArgumentException if gain <= 0
     */
    void setGain(float gain);
}