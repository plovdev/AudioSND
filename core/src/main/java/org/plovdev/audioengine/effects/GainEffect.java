package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

public class GainEffect implements AudioEffect {
    private final TrackFormat format;

    private volatile boolean isMuted = false;
    private float gain = 1;

    public GainEffect(TrackFormat format, float gain) {
        this.format = format;
        setGain(gain);
    }

    public GainEffect(TrackFormat format) {
        this(format, 1);
    }

    public float getGain() {
        return gain;
    }

    public synchronized void setGain(float gain) {
        this.gain = Math.clamp(gain, -10, 10);
    }

    @Override
    public synchronized void process(ByteBuffer input) {
        if (format == null) {
            throw new IllegalStateException("GainEffect не инициализирован");
        }

        if (!isMuted()) {
            input.order(format.byteOrder());
            input.rewind();
            int numSamples = input.remaining() / format.bytesPerSample();
            int channels = format.channels();

            for (int i = 0; i < numSamples; i++) {
                for (int ch = 0; ch < channels; ch++) {
                    int pos = input.position();
                    float sample = EffectUtils.readSample(format, input);
                    float amplified = sample * gain;
                    input.position(pos);
                    EffectUtils.writeSample(format, input, amplified);
                }
            }
        }
    }

    @Override
    public void reset() {
        gain = 1;
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
        return "Gain";
    }
}