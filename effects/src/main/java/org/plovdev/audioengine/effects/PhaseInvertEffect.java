package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

/**
 * Эффект инверсии фазы: умножает все выборки на -1.
 */
public class PhaseInvertEffect implements AudioEffect {
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

    @Override
    public void process(ByteBuffer source) {
        int frameSize = format.bytesPerSample();
        source.rewind();

        byte[] chunk = new byte[frameSize];
        for (int i = 0; i < source.limit(); i += frameSize) {
            int pos = source.position();
            source.get(chunk);
            source.position(pos);
            for (byte sample : chunk) {
                byte inverted = (byte) (-sample);
                source.put(inverted);
            }
        }
    }
}