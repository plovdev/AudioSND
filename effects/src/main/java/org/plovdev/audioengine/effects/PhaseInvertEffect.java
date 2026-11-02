package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

/**
 * Эффект инверсии фазы: умножает все выборки на -1.
 */
public class PhaseInvertEffect implements AudioEffect {
    private final TrackFormat format;
    private volatile boolean isMuted = false;

    public PhaseInvertEffect(TrackFormat format) {
        this.format = format;
    }

    @Override
    public void process(ByteBuffer source) {
        if (!isMuted()) {
            source.order(format.byteOrder());
            source.rewind();
            int numSamples = source.remaining() / format.bytesPerSample();
            int channels = format.channels();

            for (int i = 0; i < numSamples; i++) {
                for (int ch = 0; ch < channels; ch++) {
                    int pos = source.position();
                    float sample = EffectUtils.readSample(format, source);
                    source.position(pos);
                    EffectUtils.writeSample(format, source, -sample);
                }
            }
        }
    }

    @Override
    public void reset() {
        isMuted = false;
    }

    @Override
    public synchronized void setMute(boolean mute) {
        isMuted = mute;
    }

    @Override
    public boolean isMuted() {
        return isMuted;
    }

    @Override
    public String getName() {
        return "Phase Invert";
    }
}