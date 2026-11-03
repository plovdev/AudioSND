package org.plovdev.audioengine.loaders.raw;

import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.TrackExporter;
import org.plovdev.audioengine.api.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class RawTrackExporter implements TrackExporter {
    private final Logger log = LoggerFactory.getLogger(RawTrackExporter.class);

    @Override
    public void save(Track track, TrackFormat toFormat, OutputStream outputStream) {
        try {
            ByteBuffer data = track.getTrackData();
            data.rewind();

            byte[] allBytes = new byte[data.remaining()];
            data.get(allBytes);

            outputStream.write(allBytes);
            outputStream.close();

        } catch (Exception e) {
            throw new TrackExportException("RAW export failed" + e.getMessage());
        }
    }
}