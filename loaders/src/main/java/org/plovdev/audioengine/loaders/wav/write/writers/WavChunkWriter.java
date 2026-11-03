package org.plovdev.audioengine.loaders.wav.write.writers;

import org.plovdev.audioengine.format.TrackFormat;

import java.io.OutputStream;

public interface WavChunkWriter<T> {
    Class<?> getType();
    void write(OutputStream stream, TrackFormat outputFormat, T writeObject);
}