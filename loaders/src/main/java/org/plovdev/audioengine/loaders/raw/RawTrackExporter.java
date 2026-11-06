package org.plovdev.audioengine.loaders.raw;

import org.plovdev.audioengine.api.source.AudioSource;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackExporter;
import org.plovdev.audioengine.utils.TrackUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class RawTrackExporter implements TrackExporter {
    private final Logger log = LoggerFactory.getLogger(RawTrackExporter.class);

    @Override
    public void save(Track track, TrackFormat toFormat, OutputStream outputStream) {
        try {
            AudioSource source = track.getAudioSource();
            ByteBuffer data;
            while ((data = source.nextChunk()) != null) {
                data.rewind();
                byte[] allBytes = TrackUtils.byteBufferToByteArray(data);
                outputStream.write(allBytes, 0, allBytes.length);
                outputStream.close();
            }
        } catch (Exception e) {
            throw new TrackExportException("RAW export failed" + e.getMessage());
        }
    }
}