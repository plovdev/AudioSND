package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.format.TrackFormat;

import java.nio.ByteBuffer;

public class ReverseEffect implements AudioEffect {
    private final TrackFormat format;
    private volatile boolean isMuted = false;

    public ReverseEffect(TrackFormat format) {
        this.format = format;
    }

    /**
     * Метод для обработки эффекта на буффер.
     *
     * @param source исходный буффер
     */
    @Override
    public void process(ByteBuffer source) {
        if (!isMuted()) {
            source.order(format.byteOrder());
            source.rewind();
            int frameSize = format.bytesPerSample();
            int size = source.limit();
            byte[] buffer = new byte[size];
            source.get(buffer).rewind();

            for (int i = size - frameSize; i >= 0; i -= frameSize) {
                source.put(buffer, i, frameSize);
            }

            source.rewind();
        }
    }

    @Override
    public void reset() {
        isMuted = false;
    }

    @Override
    public void setMute(boolean mute) {
        isMuted = mute;
    }

    @Override
    public boolean isMuted() {
        return isMuted;
    }

    @Override
    public String getName() {
        return "Reverse";
    }
}