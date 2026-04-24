package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GainEffect implements AudioEffect {

    private TrackFormat format;
    private float gain = 0.5f;

    public GainEffect(float gain) {
        if (gain < -20 || gain > 20) {
            throw new IllegalArgumentException("Gain value is incorrect");
        }
        this.gain = gain;
    }

    public GainEffect() {
    }

    @Override
    public void setup(TrackFormat format) {
        this.format = format;
    }

    @Override
    public void process(ByteBuffer inputBuffer) {
        if (format == null) {
            throw new IllegalStateException("GainEffect не инициализирован");
        }

        int originalPosition = inputBuffer.position();
        int originalLimit = inputBuffer.limit();
        inputBuffer.rewind();

        try {
            applyGain(inputBuffer);

            inputBuffer.position(originalPosition);
            inputBuffer.limit(originalLimit);
        } catch (Exception e) {
            inputBuffer.position(originalPosition);
            inputBuffer.limit(originalLimit);
            throw e;
        }
    }

    private void applyGain(ByteBuffer input) {
        ByteOrder originalInputOrder = input.order();
        input.order(format.byteOrder());

        switch (format.audioCodec()) {
            case PCM16 -> applyGainPCM16(input);
            case PCM8 -> applyGainPCM8(input);
            case PCM24 -> applyGainPCM24(input);
            case PCM32 -> applyGainPCM32(input);
            case FLOAT32 -> applyGainFloat32(input);
            case FLOAT64 -> applyGainFloat64(input);
            default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + format.audioCodec());
        }
    }

    private void applyGainPCM16(ByteBuffer input) {
        int samples = input.capacity() / 2;

        for (int i = 0; i < samples; i++) {
            int pos = input.position();
            short sample = input.getShort();
            float normalized = sample / 32768.0f;
            float amplified = normalized * gain;
            amplified = fastTanh(amplified);
            short result = (short) (amplified * 32767.0f);
            input.position(pos);
            input.putShort(result);
        }
    }

    private void applyGainPCM8(ByteBuffer input) {
        int samples = input.capacity();

        for (int i = 0; i < samples; i++) {
            int pos = input.position();
            byte sample = input.get();
            float normalized = sample / 128.0f;
            float processed = normalized * gain;
            processed = fastTanh(processed);
            int result = Math.round(processed * 127.0f);
            if (result > 127) result = 127;
            else if (result < -128) result = -128;
            input.position(pos);
            input.put((byte) result);
        }
    }

    private void applyGainPCM24(ByteBuffer input) {
        int samples = input.capacity() / 3;

        for (int i = 0; i < samples; i++) {
            int pos = input.position();

            int b0 = input.get() & 0xFF;
            int b1 = input.get() & 0xFF;
            int b2 = input.get() & 0xFF;

            int sample = b0 | (b1 << 8) | (b2 << 16);
            if ((sample & 0x800000) != 0) {
                sample |= 0xFF000000;
            }
            float normalized = sample / 8388608.0f;
            float processed = normalized * gain;
            processed = fastTanh(processed);
            int result = Math.round(processed * 8388607.0f);
            if (result > 8388607) result = 8388607;
            else if (result < -8388608) result = -8388608;
            input.position(pos);
            input.put((byte) (result & 0xFF));
            input.put((byte) ((result >> 8) & 0xFF));
            input.put((byte) ((result >> 16) & 0xFF));
        }
    }

    private void applyGainPCM32(ByteBuffer input) {
        int samples = input.capacity() / 4;

        for (int i = 0; i < samples; i++) {
            int pos = input.position();
            int sample = input.getInt();
            double normalized = sample / 2147483648.0;
            double processed = normalized * gain;
            processed = Math.tanh(processed);
            double denormalized = processed * 2147483647.0;
            long rounded = Math.round(denormalized);
            int result = (int) rounded;
            input.position(pos);
            input.putInt(result);
        }
    }

    private void applyGainFloat32(ByteBuffer input) {
        int bytesToProcess = input.capacity();
        int samples = bytesToProcess / 4;
        for (int i = 0; i < samples; i++) {
            int pos = input.position();
            float sample = input.getFloat();
            float result = sample * gain;
            result = fastTanh(result);
            input.position(pos);
            input.putFloat(result);
        }
    }

    private void applyGainFloat64(ByteBuffer input) {
        int bytesToProcess = input.capacity();
        int samples = bytesToProcess / 8;
        for (int i = 0; i < samples; i++) {
            int pos = input.position();
            double sample = input.getDouble();
            double result = sample * gain;
            result = Math.tanh(result);
            input.position(pos);
            input.putDouble(result);
        }
    }

    private float fastTanh(float x) {
        float x2 = x * x;
        return x * (27 + x2) / (27 + 9 * x2);
    }

    public float getGain() {
        return gain;
    }

    public void setGain(float gain) {
        if (gain < -10 || gain > 10) {
            throw new IllegalArgumentException("Gain value is incorrect");
        }
        this.gain = gain;
    }
}