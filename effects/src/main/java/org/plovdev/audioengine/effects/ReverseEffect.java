package org.plovdev.audioengine.effects;

import java.nio.ByteBuffer;

public class ReverseEffect extends AudioEffect {
    private static final String EFFECT_NAME = "Reverse";

    public ReverseEffect() {
    }

    /**
     * Метод для обработки эффекта на буффер.
     *
     * @param source исходный буффер
     */
    @Override
    public void processInPlace(ByteBuffer source) {
        if (!isMuted()) {
            source.rewind();
            int size = source.limit();
            byte[] buffer = new byte[size];
            source.get(buffer).rewind();

            for (int i = size; i >= 0; i -= 4) {
                source.put(buffer, i, 4);
            }
        }
    }

    @Override
    public float processSample(float sample) {
        throw new UnsupportedOperationException("Unable to reverse one sample");
    }

    @Override
    public void reset() {
        isMuted.set(false);
    }

    @Override
    public String getName() {
        return EFFECT_NAME;
    }
}