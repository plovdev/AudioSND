package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.math.AudioMath;

import java.util.Objects;

public class ToneFilter extends AudioEffect {
    private static final String EFFECT_NAME = "Tone";
    private final AudioFormat format;
    private float cutoff;
    private float b0, b1, b2, a1, a2;
    private float x1, x2, y1, y2;
    private boolean needsUpdate = true;
    private float tone;

    public ToneFilter(AudioFormat format, float tone) {
        this.format = Objects.requireNonNull(format);
        setTone(tone);
        reset();
    }

    @Override
    public float processSample(float sample) {
        if (needsUpdate) updateCoefficients();
        float output = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;

        x2 = x1;
        x1 = sample;
        y2 = y1;
        y1 = output;

        return Math.clamp(output, -1.0f, 1.0f);
    }

    private void updateCoefficients() {
        float omega = 2.0f * (float) Math.PI * cutoff / format.sampleRate();
        float sin = (float) Math.sin(omega);
        float cos = (float) Math.cos(omega);
        float Q = 0.7f;

        float alpha = sin / (2 * Q);

        b0 = (1 - cos) / 2;
        b1 = 1 - cos;
        b2 = (1 - cos) / 2;
        a1 = -2 * cos;
        a2 = 1 - alpha;

        float a0 = 1 + alpha;
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    @Override
    public synchronized void reset() {
        isMuted.set(false);
        tone = 1;
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }

    public void setTone(float tone) {
        this.tone = AudioMath.clamp(tone, 0, 1);
        this.cutoff = 100.0f + this.tone * 7900.0f;
        this.needsUpdate = true;
    }

    public float getTone() {
        return tone;
    }
}