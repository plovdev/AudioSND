package org.plovdev.audioengine.effects;

public class GainEffect extends AudioEffect {
    private static final String EFFECT_NAME = "Gain";
    private float gain = 1;

    public GainEffect() {
        this(1);
    }

    public GainEffect(float gain) {
        setGain(gain);
    }

    public float getGain() {
        return gain;
    }

    public synchronized void setGain(float gain) {
        this.gain = Math.clamp(gain, -10, 10);
    }

    @Override
    public float processSample(float sample) {
        return sample * gain;
    }

    @Override
    public synchronized void reset() {
        isMuted.set(false);
        gain = 1;
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }
}