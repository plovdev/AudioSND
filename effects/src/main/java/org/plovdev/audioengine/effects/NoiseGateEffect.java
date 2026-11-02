package org.plovdev.audioengine.effects;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.math.AudioMath;

import java.nio.ByteBuffer;

public class NoiseGateEffect implements AudioEffect {
    private final TrackFormat format;

    private volatile boolean isMuted = false;
    private float noiseGateThreshold;
    private float envelope = 0;

    public NoiseGateEffect(TrackFormat format) {
        this(format, 0.01f);
    }

    public NoiseGateEffect(TrackFormat format, float ngt) {
        this.format = format;
        setNoiseGateThreshold(ngt);
    }

    @Override
    public void process(@NotNull ByteBuffer source) {
        if (format == null) {
            throw new IllegalStateException("Effect not initialized. Call setup() first.");
        }

        if (!isMuted()) {
            source.order(format.byteOrder());
            source.rewind();
            int totalSamples = source.remaining() / format.bytesPerSample();

            for (int i = 0; i < totalSamples; i++) {
                for (int ch = 0; ch < format.channels(); ch++) {
                    int pos = source.position();
                    float sample = EffectUtils.readSample(format, source);
                    float processed = processSample(sample);
                    source.position(pos);
                    EffectUtils.writeSample(format, source, processed);
                }
            }
        }
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
    public void reset() {
        isMuted = false;
        noiseGateThreshold = 0.02f;
        envelope = 0;
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
        return "Noise Gate";
    }

    public float getNoiseGateThreshold() {
        return noiseGateThreshold;
    }

    public void setNoiseGateThreshold(float noiseGateThreshold) {
        this.noiseGateThreshold = AudioMath.clamp(noiseGateThreshold, 0, 1);
    }
}