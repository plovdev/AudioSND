package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

public class ReverseEffect implements AudioEffect {
    private TrackFormat format;

    /**
     * Инициализирует эффект(дает базовую настройку)
     *
     * @param format формат трека
     */
    @Override
    public void setup(TrackFormat format) {
        this.format = format;
    }

    /**
     * Метод для обработки эффекта на буффер.
     *
     * @param source исходный буффер
     */
    @Override
    public void process(ByteBuffer source) {
        int frameSize = format.bytesPerSample();
        int size = source.limit();
        byte[] chunk = new byte[frameSize];

        for (int i = size - frameSize; i >= 0; i -= frameSize) {
            source.position(i);
            source.get(chunk);
            source.position(i);
            source.put(chunk);
        }
    }
}