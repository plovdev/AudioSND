package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.AudioFormat;

import java.nio.ByteBuffer;

public class GainEffect implements AudioEffect {
    private final AudioFormat format;

    private volatile boolean isMuted = false;
    private float gain = 1;

    public GainEffect(AudioFormat format, float gain) {
        this.format = format;
        setGain(gain);
    }

    public GainEffect(AudioFormat format) {
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
            throw new IllegalStateException("GainEffect not initializated");
        }

        if (!isMuted()) {
            input.rewind();

            while (input.remaining() >= 4) {
                int pos = input.position();
                float sample = input.getFloat();
                float amplified = sample * gain;
                input.position(pos);
                input.putFloat(amplified);
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