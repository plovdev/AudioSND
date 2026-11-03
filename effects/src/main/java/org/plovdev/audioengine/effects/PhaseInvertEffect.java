package org.plovdev.audioengine.effects;

/**
 * Эффект инверсии фазы: умножает все выборки на -1.
 */
public class PhaseInvertEffect extends AudioEffect {
    private static final String EFFECT_NAME = "Phase Invert";

    public PhaseInvertEffect() {
    }

    @Override
    public float processSample(float sample) {
        return -sample;
    }

    @Override
    public void reset() {
        isMuted.set(false);
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }
}