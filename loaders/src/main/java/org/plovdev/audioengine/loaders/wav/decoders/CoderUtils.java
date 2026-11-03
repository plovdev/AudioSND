package org.plovdev.audioengine.loaders.wav.decoders;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.exceptions.loaders.UnsupportedTrackFormat;
import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CoderUtils {
    @Contract(pure = true)
    public static ByteBuffer decodeToFloat32(@NonNull ByteBuffer source, TrackFormat.AudioCodec codec) {
        if (codec == TrackFormat.AudioCodec.FLOAT32) {
            return source;
        } else {
            int sampleSize = switch (codec) {
                case PCM8 -> 1;
                case PCM16 -> 2;
                case PCM24 -> 3;
                case PCM32 -> 4;
                case FLOAT64 -> 8;
                default -> throw new UnsupportedTrackFormat("Unsupported codec " + codec);
            };
            int totalSamples = source.remaining() / sampleSize;
            if (totalSamples == 0) return ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN);
            int requiredCapacity = totalSamples * 4;
            ByteBuffer dist = ByteBuffer.allocateDirect(requiredCapacity);
            dist.order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < totalSamples; i++) {
                float sample = readToFloat32(source, codec);
                dist.putFloat(sample);
            }
            return dist.flip();
        }
    }

    public static float readToFloat32(ByteBuffer buffer, TrackFormat.@NonNull AudioCodec sourceCodec) {
        return switch (sourceCodec) {
            case PCM8 -> ((buffer.get() & 0xFF) - 128.0f) / 128.0f;
            case PCM16 -> buffer.getShort() / 32768.0f;
            case PCM24 -> {
                int b1 = buffer.get() & 0xFF;
                int b2 = buffer.get() & 0xFF;
                int b3 = buffer.get();
                int val = b1 | (b2 << 8) | (b3 << 16);
                yield val / 8388608.0f;
            }
            case PCM32 -> buffer.getInt() / 2147483648.0f;
            case FLOAT32 -> buffer.getFloat();
            case FLOAT64 -> (float) buffer.getDouble();
            default -> throw new UnsupportedTrackFormat("Unsupported codec " + sourceCodec);
        };
    }

    public static void writeToCodec(ByteBuffer buffer, float sample, TrackFormat.@NonNull AudioCodec sourceCodec) {
        float clipped = Math.max(-1.0f, Math.min(1.0f, sample));

        switch (sourceCodec) {
            case PCM8 -> {
                int val = Math.round((clipped * 128.0f) + 128.0f);
                val = Math.max(0, Math.min(255, val));
                buffer.put((byte) val);
            }
            case PCM16 -> {
                int val = Math.round(clipped * 32768.0f);
                val = Math.max(-32768, Math.min(32767, val));
                buffer.putShort((short) val);
            }
            case PCM24 -> {
                int val = Math.round(clipped * 8388608.0f);
                val = Math.max(-8388608, Math.min(8388607, val));
                buffer.put((byte) (val & 0xFF));
                buffer.put((byte) ((val >> 8) & 0xFF));
                buffer.put((byte) ((val >> 16) & 0xFF));
            }
            case PCM32 -> {
                long val = Math.round((double) clipped * 2147483648.0);
                val = Math.max(-2147483648L, Math.min(2147483647L, val));
                buffer.putInt((int) val);
            }
            case FLOAT32 -> buffer.putFloat(clipped);
            case FLOAT64 -> buffer.putDouble(clipped);
            default -> throw new UnsupportedTrackFormat("Unsupported codec " + sourceCodec);
        }
    }
}