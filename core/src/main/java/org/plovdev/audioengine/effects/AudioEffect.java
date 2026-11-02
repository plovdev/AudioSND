package org.plovdev.audioengine.effects;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Audio effect for real-time stream processing.
 * <p>
 * Implementation may allocate native resources during setup
 * and release them during finalization or explicitly via implementation-specific method.
 */
public interface AudioEffect {
    void process(@NotNull ByteBuffer source);

    void reset();

    void setMute(boolean mute);

    boolean isMuted();

    String getName();
}