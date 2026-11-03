package org.plovdev.audioengine.loaders.wav.decoders;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.Decoder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IMAADPCMDecoder implements Decoder {
    private static final short[] STEP_TABLE = {
            7, 8, 9, 10, 11, 12, 13, 14,
            16, 17, 19, 21, 23, 25, 28, 31,
            34, 37, 41, 45, 50, 55, 60, 66,
            73, 80, 88, 97, 107, 118, 130, 143,
            157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658,
            724, 796, 876, 963, 1060, 1166, 1282, 1411,
            1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
            3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484,
            7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794,
            32767
    };
    private static final int[] INDEX_TABLE = {
            -1, -1, -1, -1, 2, 4, 6, 8,
            -1, -1, -1, -1, 2, 4, 6, 8
    };

    private final int blockSize; // Размер блока в байтах (например, 256 или 512)

    public IMAADPCMDecoder(int blockSize) {
        this.blockSize = blockSize;
    }

    // Декодирует один nibble
    private short decodeNibble(int nibble, short @NonNull [] state) {
        int step = STEP_TABLE[state[1]];
        int delta = step >> 3;

        if ((nibble & 1) != 0) delta += step >> 2;
        if ((nibble & 2) != 0) delta += step >> 1;
        if ((nibble & 4) != 0) delta += step;
        if ((nibble & 8) != 0) delta = -delta;

        int sample = state[0] + delta;

        // Клиппинг
        if (sample > 32767) sample = 32767;
        else if (sample < -32768) sample = -32768;

        state[0] = (short) sample;

        state[1] += (short) INDEX_TABLE[nibble & 0x0F];
        if (state[1] < 0) state[1] = 0;
        if (state[1] > 88) state[1] = 88;

        return (short) sample;
    }

    @Override
    public ByteBuffer decode(@NonNull ByteBuffer input) {
        input.rewind();

        if (input.remaining() < 4) {
            return ByteBuffer.allocate(0);
        }

        int blockCount = input.remaining() / blockSize;
        if (blockCount == 0) {
            return ByteBuffer.allocate(0);
        }

        int samplesPerBlock = 1 + ((blockSize - 4) * 2);
        int totalSamples = blockCount * samplesPerBlock;

        ByteBuffer result = ByteBuffer.allocateDirect(totalSamples * 2)
                .order(ByteOrder.LITTLE_ENDIAN);

        for (int block = 0; block < blockCount; block++) {
            if (input.remaining() < blockSize) {
                break;
            }

            short firstSample = input.getShort();
            int initialStepIndex = input.get() & 0xFF;
            input.get(); // reserved byte

            short[] state = new short[2];
            state[0] = firstSample;
            state[1] = (short) Math.min(initialStepIndex, 88);

            // Сохраняем первый сэмпл
            result.putShort(firstSample);

            int bytesInBlockData = blockSize - 4;
            for (int i = 0; i < bytesInBlockData; i++) {
                byte b = input.get();

                int highNibble = (b >> 4) & 0x0F;
                result.putShort(decodeNibble(highNibble, state));

                // Декодируем младший nibble (4 бита)
                int lowNibble = b & 0x0F;
                result.putShort(decodeNibble(lowNibble, state));
            }
        }

        return CoderUtils.decodeToFloat32(result.flip(), TrackFormat.AudioCodec.PCM16);
    }
}