package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

public final class EffectUtils {
    private EffectUtils() {
    }

    public static float readSample(TrackFormat format, ByteBuffer buffer) {
        int bitDepth = format.bitDepth();
        return switch (bitDepth) {
            case 8 -> format.signed() ? buffer.get() / 127.0f : (buffer.get() - 128) / 128.0f;
            case 16 -> buffer.getShort() / 32767.0f;
            case 24 -> {
                byte b1 = buffer.get();
                byte b2 = buffer.get();
                byte b3 = buffer.get();
                int sample24;
                if (format.byteOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    sample24 = ((b3 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b1 & 0xFF);
                } else {
                    sample24 = ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | (b3 & 0xFF);
                }
                yield format.signed() ? sample24 / 8388607.0f : (sample24 - 8388608) / 8388608.0f;
            }
            case 32 -> format.audioCodec() == TrackFormat.AudioCodec.FLOAT32 ? buffer.getFloat() : buffer.getInt() / 2147483647.0f;
            case 64 -> (float) buffer.getDouble();
            default -> throw new IllegalArgumentException("Unsupported bit depth: " + bitDepth);
        };
    }

    public static void writeSample(TrackFormat format, ByteBuffer buffer, float sample) {
        sample = Math.clamp(sample, -1.0f, 1.0f);
        switch (format.bitDepth()) {
            case 8:
                buffer.put((byte) (format.signed() ? sample * 127.0f : sample * 128.0f + 128.0f));
                break;
            case 16:
                buffer.putShort((short) (sample * 32767.0f));
                break;
            case 24:
                int sample24 = (int) (sample * (format.signed() ? 8388607.0f : 8388608.0f));
                if (format.signed() && (sample24 & 0x800000) != 0) {
                    sample24 |= 0xFF000000;
                }

                if (!format.signed()) sample24 += 8388608;
                if (format.byteOrder() == java.nio.ByteOrder.LITTLE_ENDIAN) {
                    buffer.put((byte) (sample24 & 0xFF));
                    buffer.put((byte) ((sample24 >> 8) & 0xFF));
                    buffer.put((byte) ((sample24 >> 16) & 0xFF));
                } else {
                    buffer.put((byte) ((sample24 >> 16) & 0xFF));
                    buffer.put((byte) ((sample24 >> 8) & 0xFF));
                    buffer.put((byte) (sample24 & 0xFF));
                }
                break;
            case 32:
                if (format.audioCodec() == TrackFormat.AudioCodec.FLOAT32) {
                    buffer.putFloat(sample);
                } else {
                    buffer.putInt((int) (sample * 2147483647.0f));
                }
                break;
            case 64:
                buffer.putDouble(sample);
                break;
        }
    }
}