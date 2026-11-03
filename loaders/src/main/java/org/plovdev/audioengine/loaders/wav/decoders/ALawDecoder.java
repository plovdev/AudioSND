package org.plovdev.audioengine.loaders.wav.decoders;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.Decoder;

import java.nio.ByteBuffer;

public class ALawDecoder implements Decoder {
    // Таблица декдирования alaw
    private static final short[] TABLE = new short[256];
    // раз за работу движка
    static {
        for (int i = 0; i < 256; i++) {
            // заполняем таблицу
            TABLE[i] = decodeSample((byte) i);
        }
    }

    public ALawDecoder() {
    }

    private static short decodeSample(byte alawByte) {
        int BIAS = 0x55;
        alawByte = (byte) (alawByte ^ BIAS);

        int sign = alawByte & 0x80;
        int exponent = (alawByte >> 4) & 0x07;
        int mantissa = alawByte & 0x0F;

        int sample;
        if (exponent == 0) {
            sample = (mantissa << 1) + 1;
        } else {
            sample = ((mantissa << 1) + 33) << (exponent - 1);
        }
        sample -= BIAS;

        return (short) (sign != 0 ? -sample : sample);
    }

    private short decode(byte alaw) {
        return TABLE[alaw & 0xFF];
    }

    @Override
    public ByteBuffer decode(@NonNull ByteBuffer alawData) {
        alawData.rewind();

        int samples = alawData.remaining();
        ByteBuffer pcm = ByteBuffer.allocateDirect(samples * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samples; i++) {
            pcm.putShort(decode(alawData.get()));
        }

        return CoderUtils.decodeToFloat32(pcm, TrackFormat.AudioCodec.PCM16);
    }
}