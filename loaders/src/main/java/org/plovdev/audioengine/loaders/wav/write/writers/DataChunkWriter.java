package org.plovdev.audioengine.loaders.wav.write.writers;

import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.TrackFormat;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.plovdev.audioengine.loaders.ExportUtils.intToLittleEndian;
import static org.plovdev.audioengine.loaders.wav.struct.WavChunkId.DATA;

public class DataChunkWriter implements WavChunkWriter<Track> {
    @Override
    public Class<?> getType() {
        return Track.class;
    }

    @Override
    public void write(OutputStream outputStream, TrackFormat outputFormat, Track track) {
        try {
            ByteBuffer trackData = track.getTrackData();

            int bitDepth = outputFormat.bitDepth();
            int dataChunkSize = trackData.remaining();

            outputStream.write(DATA.getChunk().getBytes());
            outputStream.write(intToLittleEndian(dataChunkSize));

            byte[] bytes = new byte[trackData.remaining()];
            trackData.get(bytes);
            outputStream.write(bytes);

            if (dataChunkSize % 2 != 0) {
                outputStream.write(0);
            }
        } catch (Exception e) {
            throw new TrackExportException(e.getMessage());
        }
    }
}
