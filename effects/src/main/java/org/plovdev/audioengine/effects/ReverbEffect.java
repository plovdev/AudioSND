package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

public class ReverbEffect implements AudioEffect {

    public enum Type {
        ROOM, HALL, PLATE, SPRING, CATHEDRAL
    }

    private TrackFormat format;
    private Type type;
    private float roomSize;
    private float decayTime;
    private float damping;
    private float mix;

    // Константы для алгоритма Schroeder reverb
    private static final int[] COMB_TUNINGS = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    private static final int[] ALLPASS_TUNINGS = {225, 341, 441, 556};
    private static final float COMB_GAIN_BASE = 0.001f;

    private float[][] combBuffers;
    private int[] combIndices;
    private float[] combFeedback;

    private float[][] allpassBuffers;
    private int[] allpassIndices;

    private float sampleRate;
    private int channels;

    public ReverbEffect() {
        this(Type.HALL, 1f, 1f, 0.5f, 0.5f);
    }

    public ReverbEffect(Type type, float roomSize, float decayTime, float damping, float mix) {
        this.type = type;
        this.roomSize = clamp(roomSize, 0.0f, 1.0f);
        this.decayTime = clamp(decayTime, 0.1f, 10.0f);
        this.damping = clamp(damping, 0.0f, 1.0f);
        this.mix = clamp(mix, 0.0f, 1.0f);
    }

    @Override
    public void setup(TrackFormat format) {
        this.format = format;
        this.sampleRate = format.sampleRate();
        this.channels = format.channels();
        initializeBuffers();
    }

    private void initializeBuffers() {
        float sizeMultiplier = 0.5f + roomSize * 1.5f;
        int numCombs = type == Type.CATHEDRAL ? 8 :
                type == Type.HALL ? 6 :
                        4;

        int numAllpasses = type == Type.CATHEDRAL ? 4 : 2;

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

    @Override
    public void process(ByteBuffer source) {
        if (format == null) {
            throw new IllegalStateException("Effect not initialized. Call setup() first.");
        }

        source.order(format.byteOrder());
        source.rewind();

        int bytesPerSample = format.bytesPerSample() / channels;
        int numSamples = source.remaining() / format.bytesPerSample();

        for (int i = 0; i < numSamples; i++) {
            for (int ch = 0; ch < channels; ch++) {
                int pos = source.position();
                float input = readSample(source, bytesPerSample);
                float output = processSample(input, ch);
                source.position(pos);
                writeSample(source, output, bytesPerSample);
            }
        }
    }

    private float processSample(float input, int channel) {
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
        return input * (1.0f - mix) + wetSignal * mix;
    }

    private float readSample(ByteBuffer buffer, int bytesPerSample) {
        switch (bytesPerSample) {
            case 1:
                return format.signed() ?
                        buffer.get() / 127.0f :
                        (buffer.get() - 128) / 128.0f;
            case 2:
                return buffer.getShort() / 32767.0f;
            case 3:
                byte b1 = buffer.get();
                byte b2 = buffer.get();
                byte b3 = buffer.get();
                int sample24 = ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
                if (!format.signed()) sample24 -= 0x800000;
                return sample24 / 8388608.0f;
            case 4:
                if (format.audioCodec() == TrackFormat.AudioCodec.FLOAT32) {
                    return buffer.getFloat();
                } else {
                    return buffer.getInt() / 2147483647.0f;
                }
            case 8:
                return (float) buffer.getDouble();
            default:
                throw new IllegalArgumentException("Unsupported sample size: " + bytesPerSample);
        }
    }

    private void writeSample(ByteBuffer buffer, float sample, int bytesPerSample) {
        sample = clamp(sample, -1.0f, 1.0f);

        switch (bytesPerSample) {
            case 1:
                buffer.put((byte) (format.signed() ?
                        sample * 127.0f :
                        sample * 128.0f + 128.0f));
                break;
            case 2:
                buffer.putShort((short) (sample * 32767.0f));
                break;
            case 3:
                int sample24 = (int) (sample * 8388608.0f);
                if (!format.signed()) sample24 += 0x800000;
                buffer.put((byte) sample24);
                buffer.put((byte) (sample24 >> 8));
                buffer.put((byte) (sample24 >> 16));
                break;
            case 4:
                if (format.audioCodec() == TrackFormat.AudioCodec.FLOAT32) {
                    buffer.putFloat(sample);
                } else {
                    buffer.putInt((int) (sample * 2147483647.0f));
                }
                break;
            case 8:
                buffer.putDouble(sample);
                break;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public void setType(Type type) {
        this.type = type;
        if (format != null) initializeBuffers();
    }

    public void setRoomSize(float roomSize) {
        this.roomSize = clamp(roomSize, 0.0f, 1.0f);
        if (format != null) initializeBuffers();
    }

    public void setDecayTime(float decayTime) {
        this.decayTime = clamp(decayTime, 0.1f, 10.0f);
        if (format != null) initializeBuffers();
    }

    public void setDamping(float damping) {
        this.damping = clamp(damping, 0.0f, 1.0f);
        if (format != null) initializeBuffers();
    }

    public void setMix(float mix) {
        this.mix = clamp(mix, 0.0f, 1.0f);
    }
}