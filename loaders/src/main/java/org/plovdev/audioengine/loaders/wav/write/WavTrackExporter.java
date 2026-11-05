package org.plovdev.audioengine.loaders.wav.write;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackExporter;
import org.plovdev.audioengine.loaders.wav.struct.WavChunkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static org.plovdev.audioengine.loaders.ExportUtils.intToLittleEndian;

public class WavTrackExporter implements TrackExporter {
    public static final String RIFF = "RIFF";
    public static final String WAVE = "WAVE";

    private final Logger log = LoggerFactory.getLogger(WavTrackExporter.class);

    @Override
    public void save(Track track, TrackFormat outputFormat, OutputStream outputStream) {
        try {
            ByteBuffer trackData = track.getTrackData().rewind();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            WavChunkWriterManager chunkWriter = new WavChunkWriterManager(buffer);

            chunkWriter.writeChunk(WavChunkId.FMT, outputFormat, outputFormat);
            int fmtSize = buffer.size();

            int listSize = 0;
            if (track.getMetaData() != null && !track.getMetaData().isEmpty()) {
                chunkWriter.writeChunk(WavChunkId.ID3_HEAD, outputFormat, track.getMetaData());
                listSize = buffer.size() - fmtSize;
            }

            chunkWriter.writeChunk(WavChunkId.DATA, outputFormat, track);
            int dataSize = buffer.size() - fmtSize - listSize;

            int totalChunkSize = 4 +
                    8 + fmtSize +
                    8 + dataSize +
                    (listSize > 0 ? (8 + listSize) : 0);

            outputStream.write(RIFF.getBytes());
            outputStream.write(intToLittleEndian(totalChunkSize));
            outputStream.write(WAVE.getBytes());

            buffer.writeTo(outputStream);
            outputStream.close();
        } catch (Exception e) {
            throw new TrackExportException(e.getMessage());
        }
    }
}