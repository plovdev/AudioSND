package org.plovdev.audioengine.effects;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio effect for real-time stream processing.
 * <p>
 * Implementation may allocate native resources during setup
 * and release them during finalization or explicitly via implementation-specific method.
 */
public abstract class AudioEffect {
    protected final AtomicBoolean isMuted = new AtomicBoolean(false);

    public ByteBuffer processToNew(ByteBuffer source) {
        processInPlace(source);
        return source;
    }

    public void processInPlace(@NotNull ByteBuffer source) {
        if (!isMuted()) {
            source.rewind();

            while (source.remaining() >= 4) {
                int pos = source.position();
                float sample = source.getFloat();
                float processed = processSample(sample);
                source.position(pos);
                source.putFloat(processed);
            }
        }
    }

    public abstract float processSample(float sample);

    public abstract void reset();

    public void setMute(boolean mute) {
        isMuted.set(mute);
    }

    public boolean isMuted() {
        return isMuted.get();
    }

    public abstract String getName();
}