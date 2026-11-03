package org.plovdev.audioengine.effects;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.AudioFormat;

import java.util.Objects;

import static org.plovdev.audioengine.math.AudioMath.clamp;

public class ReverbEffect extends AudioEffect {
    private static final String EFFECT_NAME = "Reverb";

    private static final int[] COMB_TUNINGS = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    private static final int[] ALLPASS_TUNINGS = {225, 341, 441, 556};
    private static final float COMB_GAIN_BASE = 0.001f;

    public enum RoomType {
        ROOM, HALL, PLATE, SPRING, CATHEDRAL
    }

    private final float sampleRate;
    private final int channels;
    private int channel = 0;

    private RoomType type;
    private float roomSize;
    private float decayTime;
    private float damping;
    private float mix;
    private float[][] combBuffers;
    private int[] combIndices;
    private float[] combFeedback;
    private float[][] allpassBuffers;
    private int[] allpassIndices;

    public ReverbEffect(AudioFormat format) {
        this(format, RoomType.HALL, 1f, 1f, 0.5f, 0.5f);
    }

    public ReverbEffect(@NonNull AudioFormat format, RoomType type, float roomSize, float decayTime, float damping, float mix) {
        Objects.requireNonNull(format);
        this.type = type;
        this.roomSize = clamp(roomSize, 0.0f, 1.0f);
        this.decayTime = clamp(decayTime, 0.1f, 10.0f);
        this.damping = clamp(damping, 0.0f, 1.0f);
        this.mix = clamp(mix, 0.0f, 1.0f);

        this.sampleRate = format.sampleRate();
        this.channels = format.channels();
        initializeBuffers();
    }

    @Override
    public synchronized void reset() {
        isMuted.set(false);
        type = RoomType.HALL;
        roomSize = 1;
        decayTime = 1;
        damping = 0.5f;
        mix = 0.5f;
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }

    @Override
    public float processSample(float input) {
        float earlyReflections = input * 0.3f;

        float combSum = 0;
        for (int i = 0; i < combBuffers.length; i++) {
            int bufferIndex = combIndices[i] + channel;
            float delayed = combBuffers[i][bufferIndex];
            combBuffers[i][bufferIndex] = input + delayed * combFeedback[i];
            float filtered = delayed * (1.0f - damping) + delayed * damping * 0.7f;
            combSum += filtered;
            combIndices[i] = (combIndices[i] + channels) % (combBuffers[i].length - channels + 1);
        }

        float allpassOut = combSum / combBuffers.length;
        for (int i = 0; i < allpassBuffers.length; i++) {
            int bufferIndex = allpassIndices[i] + channel;
            float delayed = allpassBuffers[i][bufferIndex];

            float allpassFeedback = 0.5f;
            float output = -allpassFeedback * allpassOut + delayed;
            allpassBuffers[i][bufferIndex] = allpassOut + allpassFeedback * output;

            allpassOut = output;

            allpassIndices[i] = (allpassIndices[i] + channels) % (allpassBuffers[i].length - channels + 1);
        }

        float wetSignal = earlyReflections * 0.2f + allpassOut * 0.8f;

        if (channel == channels - 1) {
            channel = 0;
        } else {
            channel++;
        }

        return input * (1.0f - mix) + wetSignal * mix;
    }

    public void setType(RoomType type) {
        this.type = Objects.requireNonNull(type);
        initializeBuffers();
    }

    public void setRoomSize(float roomSize) {
        this.roomSize = clamp(roomSize, 0.0f, 1.0f);
        initializeBuffers();
    }

    public void setDecayTime(float decayTime) {
        this.decayTime = clamp(decayTime, 0.1f, 10.0f);
        initializeBuffers();
    }

    public void setDamping(float damping) {
        this.damping = clamp(damping, 0.0f, 1.0f);
        initializeBuffers();
    }

    public void setMix(float mix) {
        this.mix = clamp(mix, 0.0f, 1.0f);
    }

    private void initializeBuffers() {
        float sizeMultiplier = 0.5f + roomSize * 1.5f;
        int numCombs = type == RoomType.CATHEDRAL ? 8 : type == RoomType.HALL ? 6 : 4;
        int numAllpasses = type == RoomType.CATHEDRAL ? 4 : 2;

        combBuffers = new float[numCombs][];
        combIndices = new int[numCombs];
        combFeedback = new float[numCombs];

        for (int i = 0; i < numCombs; i++) {
            int delay = (int) (COMB_TUNINGS[i % COMB_TUNINGS.length] * sizeMultiplier);
            combBuffers[i] = new float[delay * channels];
            combIndices[i] = 0;
            float g = (float) Math.pow(10, -3.0 * delay / (sampleRate * decayTime));
            combFeedback[i] = g * (1.0f - damping * 0.3f);
        }

        allpassBuffers = new float[numAllpasses][];
        allpassIndices = new int[numAllpasses];

        for (int i = 0; i < numAllpasses; i++) {
            int delay = (int) (ALLPASS_TUNINGS[i % ALLPASS_TUNINGS.length] * sizeMultiplier * 0.5f);
            allpassBuffers[i] = new float[delay * channels];
            allpassIndices[i] = 0;
        }
    }
}