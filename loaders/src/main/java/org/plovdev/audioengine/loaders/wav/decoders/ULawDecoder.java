package org.plovdev.audioengine.loaders.wav.decoders;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.Decoder;

import java.nio.ByteBuffer;

public class ULawDecoder implements Decoder {
    // Таблица декдирования ulaw
    private static final short[] TABLE = new short[256];

    // раз за работу движка
    static {
        for (int i = 0; i < 256; i++) {
            // заполняем таблицу
            TABLE[i] = decodeSample((byte) i);
        }
    }

    public ULawDecoder() {
    }

    private static short decodeSample(byte ulawByte) {
        ulawByte = (byte) ~ulawByte;

        int sign = ulawByte & 0x80;
        int exponent = (ulawByte >> 4) & 0x07;
        int mantissa = ulawByte & 0x0F;

        // 132
        int BIAS = 0x84;
        int sample = ((mantissa << 3) + BIAS) << exponent;
        sample -= BIAS;

        return (short) (sign != 0 ? -sample : sample);
    }

    private short decode(byte ulaw) {
        return TABLE[ulaw & 0xFF];
    }

    @Override
    public ByteBuffer decode(@NonNull ByteBuffer ulawData) {
        ulawData.rewind();

        int samples = ulawData.remaining();
        ByteBuffer pcm = ByteBuffer.allocateDirect(samples * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < samples; i++) {
            pcm.putShort(decode(ulawData.get()));
        }

        return CoderUtils.decodeToFloat32(pcm.flip(), TrackFormat.AudioCodec.PCM16);
    }
}