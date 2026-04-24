package org.plovdev.audioengine.effects;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

/**
 * Audio effect for real-time stream processing.
 * <p>
 * Typical lifecycle:
 * <ol>
 *     <li>Call {@link #setup(TrackFormat)} once</li>
 *     <li>Call {@link #process(ByteBuffer)} repeatedly with audio chunks</li>
 * </ol>
 * </p>
 * Implementation may allocate native resources during setup
 * and release them during finalization or explicitly via implementation-specific method.
 */
public interface AudioEffect {

    /**
     * Initializes the effect with the specified audio format.
     * Must be called exactly once before any {@link #process(ByteBuffer)} calls.
     *
     * @param format audio format (sample rate, channels, etc.)
     * @throws IllegalStateException if already initialized
     */
    void setup(@NotNull TrackFormat format);

    void process(@NotNull ByteBuffer source);
}