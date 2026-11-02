package org.plovdev.audioengine.effects;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.math.AudioMath;

import java.nio.ByteBuffer;

public class ToneFilter implements AudioEffect {
    private final TrackFormat format;
    private volatile boolean isMuted;
    private float cutoff;
    private float b0, b1, b2, a1, a2;
    private float x1, x2, y1, y2;
    private boolean needsUpdate = true;
    private float tone;

    public ToneFilter(TrackFormat format, float tone) {
        this.format = format;
        setTone(tone);
        reset();
    }

    @Override
    public void process(@NotNull ByteBuffer source) {
        if (format == null) {
            throw new IllegalStateException("ToneFilter не инициализирован");
        }

        if (!isMuted()) {
            source.order(format.byteOrder());
            source.rewind();
            int numSamples = source.remaining() / format.bytesPerSample();
            int channels = format.channels();

            for (int i = 0; i < numSamples; i++) {
                for (int ch = 0; ch < channels; ch++) {
                    int pos = source.position();
                    float sample = EffectUtils.readSample(format, source);
                    float output = processSample(sample);
                    source.position(pos);
                    EffectUtils.writeSample(format, source, output);
                }
            }
        }
    }

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
        return "Tone";
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