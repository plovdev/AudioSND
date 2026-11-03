package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.math.AudioMath;

public class NoiseGateEffect extends AudioEffect {
    private static final String EFFECT_NAME = "Noise Gate";
    private float envelope = 0;
    private volatile float noiseGateThreshold;

    public NoiseGateEffect() {
    }

    public NoiseGateEffect(float ngt) {
        setNoiseGateThreshold(ngt);
    }

    public float processSample(float sample) {
        float absSample = Math.abs(sample);
        float release = 0.0005f;

        if (absSample > envelope) {
            envelope = absSample;
        } else {
            envelope = envelope * (1 - release) + absSample * release;
        }
        if (envelope < noiseGateThreshold) {
            float factor = envelope / noiseGateThreshold;
            return sample * factor * factor;
        }

        return sample;
    }

    @Override
    public synchronized void reset() {
        isMuted.set(false);
        noiseGateThreshold = 0.02f;
        envelope = 0;
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }

    public float getNoiseGateThreshold() {
        return noiseGateThreshold;
    }

    public void setNoiseGateThreshold(float noiseGateThreshold) {
        this.noiseGateThreshold = AudioMath.clamp(noiseGateThreshold, 0, 1);
    }
}