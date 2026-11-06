package org.plovdev.audioengine.loaders.wav.write.writers;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.TrackFormat;

import java.io.OutputStream;

@Deprecated
public class DataChunkWriter implements WavChunkWriter<Track> {
    @Override
    public Class<?> getType() {
        return Track.class;
    }

    @Override
    public void write(OutputStream outputStream, TrackFormat outputFormat, Track track) {

    }
}
