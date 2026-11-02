package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

import static org.plovdev.audioengine.math.AudioMath.clamp;

/**
 * Улучшенный эффект перегруза для гитары с шумоподавителем и оптимизациями
 *
 * @author Anton
 * @version 2.0
 */
public class OverdriveEffect implements AudioEffect {
    private static final float MIN_DRIVE = 0.0f;
    private static final float MAX_DRIVE = 2.0f;
    private static final float MIN_LEVEL = 0.0f;
    private static final float MAX_LEVEL = 2.0f;
    private static final float MIN_MIX = 0.0f;
    private static final float MAX_MIX = 1.0f;

    public enum OverdriveType {SOFT_CLIPPING, HARD_CLIPPING, TUBE_SIMULATION, FUZZ}

    private final TrackFormat format;
    private volatile boolean isMuted = false;
    private OverdriveType type;
    private float drive;
    private float level;
    private float mix;

    private final ToneFilter toneFilter;
    private final NoiseGateEffect noiseGate;

    private float previousSample;
    private final int channels;


    public OverdriveEffect(TrackFormat format) {
        this(format, OverdriveType.FUZZ, 1f, 0.3f, 1f, 0.6f, 0.0f);
    }

    public OverdriveEffect(TrackFormat format, OverdriveType type, float drive, float tone, float level, float mix, float noisegate) {
        this.format = format;
        this.toneFilter = new ToneFilter(format, tone);
        this.noiseGate = new NoiseGateEffect(format, noisegate);
        this.type = type;
        this.drive = clamp(drive, MIN_DRIVE, MAX_DRIVE);
        this.level = clamp(level, MIN_LEVEL, MAX_LEVEL);
        this.mix = clamp(mix, MIN_MIX, MAX_MIX);
        this.previousSample = 0.0f;
        this.channels = format.channels();
        reset();
    }

    /**
     * Сброс состояния эффекта (для новой обработки)
     */
    @Override
    public void reset() {
        previousSample = 0.0f;
        isMuted = false;
        toneFilter.reset();
        noiseGate.reset();
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
        return "Overdrive";
    }

    @Override
    public synchronized void process(ByteBuffer buffer) {
        if (format == null) {
            throw new IllegalStateException("Effect not initialized. Call setup() first.");
        }
        if (buffer == null || !buffer.hasRemaining() || isMuted()) {
            return;
        }

        buffer.order(format.byteOrder());
        buffer.rewind();
        int numSamples = buffer.remaining() / format.bytesPerSample();

        for (int i = 0; i < numSamples; i++) {
            for (int ch = 0; ch < channels; ch++) {
                int pos = buffer.position();
                float sample = EffectUtils.readSample(format, buffer);
                float processed = processSample(sample);
                buffer.position(pos);
                EffectUtils.writeSample(format, buffer, processed);
            }
        }
    }

    private float processSample(float sample) {
        float gated = noiseGate.processSample(sample);
        float distorted = toneFilter.processSample(applyDistortion(gated));
        float mixed = sample * (1 - mix) + distorted * mix;
        mixed *= level;

        return preventDenormal(mixed);
    }

    private float applyDistortion(float sample) {
        float amplified = sample * (1 + drive * 8);

        return switch (type) {
            case SOFT_CLIPPING -> softClipping(amplified);
            case HARD_CLIPPING -> hardClipping(amplified);
            case TUBE_SIMULATION -> tubeSimulation(amplified);
            case FUZZ -> fuzzDistortion(amplified);
        };
    }

    private float softClipping(float input) {
        float threshold = 0.8f;
        float knee = 0.15f;
        float tpk = threshold + knee;
        float tmk = threshold - knee;

        if (input > tpk) {
            float diff = input - tpk;
            return threshold + diff / (1 + diff * diff);
        } else if (input > tmk) {
            float diff = input - threshold;
            float ratio = diff * diff / (3 * knee * knee);
            return threshold + diff * (1 - ratio);
        } else if (input < -tpk) {
            float diff = -input - tpk;
            return -threshold - diff / (1 + diff * diff);
        } else if (input < -tmk) {
            float diff = -input - threshold;
            float ratio = diff * diff / (3 * knee * knee);
            return -threshold - diff * (1 - ratio);
        }
        return input;
    }

    private float hardClipping(float input) {
        float threshold = 0.7f;
        return clamp(input, -threshold, threshold);
    }

    private float tubeSimulation(float input) {
        float gain = 2.5f;
        float tanhGain = (float) Math.tanh(gain);
        float shaped = (float) Math.tanh(input * gain) / tanhGain;

        if (shaped > 0.5f) {
            shaped = 0.5f + (shaped - 0.5f) * 0.7f;
        } else if (shaped < -0.5f) {
            shaped = -0.5f + (shaped + 0.5f) * 0.7f;
        }

        return shaped;
    }

    private float fuzzDistortion(float input) {
        final float hysteresis = 0.03f;
        float distorted;

        if (input > hysteresis) {
            distorted = 1.0f;
        } else if (input < -hysteresis) {
            distorted = -1.0f;
        } else {
            float t = input / hysteresis;
            distorted = t * 0.5f;
        }

        float octaveUp = Math.abs(input) * 0.4f;
        distorted = distorted * 0.8f + octaveUp * 0.2f;
        float smoothFactor = 0.85f - drive * 0.1f;
        distorted = smoothFactor * previousSample + (1 - smoothFactor) * distorted;
        previousSample = distorted;

        return clamp(distorted, -0.98f, 0.98f);
    }

    private float preventDenormal(float sample) {
        if (Math.abs(sample) < 1e-25f) {
            return 0.0f;
        }
        return sample;
    }

    public void setDrive(float drive) {
        this.drive = clamp(drive, MIN_DRIVE, MAX_DRIVE);
    }

    public void setTone(float tone) {
        toneFilter.setTone(clamp(tone, 0, 1));
    }

    public void setLevel(float level) {
        this.level = clamp(level, MIN_LEVEL, MAX_LEVEL);
    }

    public void setMix(float mix) {
        this.mix = clamp(mix, MIN_MIX, MAX_MIX);
    }

    public void setNoiseGate(float ngt) {
        noiseGate.setNoiseGateThreshold(clamp(ngt, 0, 1));
    }

    public void setType(OverdriveType type) {
        this.type = type;
        reset();
    }
}