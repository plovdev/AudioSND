package org.plovdev.audioengine.loaders.wav;

import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.io.TrackOutputStream;
import org.plovdev.audioengine.loaders.wav.struct.WavChunkId;
import org.plovdev.audioengine.loaders.wav.write.WavChunkWriterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.plovdev.audioengine.loaders.wav.struct.WavChunkId.DATA;
import static org.plovdev.audioengine.loaders.wav.write.WavTrackExporter.RIFF;
import static org.plovdev.audioengine.loaders.wav.write.WavTrackExporter.WAVE;

public class WavTrackOutputStream extends TrackOutputStream {
    private static final Logger log = LoggerFactory.getLogger(WavTrackOutputStream.class);
    private long dataSizePosition = -1;
    private FileChannel fileChannel;

    public WavTrackOutputStream(TrackFormat trackFormat, FileOutputStream toWrite) {
        super(trackFormat, toWrite);
        log.info("create fch");
    }

    @Override
    public void writeHead() throws IOException {
        toWrite.write(RIFF.getBytes()); // "RIFF"
        toWrite.write(new byte[4]);    // Заглушка
        toWrite.write(WAVE.getBytes()); // "WAVE"

        ByteArrayOutputStream fmtBuffer = new ByteArrayOutputStream();
        WavChunkWriterManager manager = new WavChunkWriterManager(fmtBuffer);
        manager.writeChunk(WavChunkId.FMT, trackFormat);
        fmtBuffer.writeTo(toWrite);
        toWrite.write(DATA.getChunk().getBytes());
        toWrite.flush();

        if (toWrite instanceof FileOutputStream fos) {
            fileChannel = fos.getChannel();
            this.dataSizePosition = fileChannel.position();
        }
        toWrite.write(new byte[4]);
        toWrite.flush();
    }


    @Override
    protected void writeEnd() throws IOException {
        long finalPosition = fileChannel.position();
        try {
            long dataSize = finalPosition - dataSizePosition - 4;
            fileChannel.position(dataSizePosition);
            writeLEInt(dataSize);
            long riffSize = finalPosition - 8;
            fileChannel.position(4);
            writeLEInt(riffSize);
            log.debug("WAV updated: data={}, riff={}", dataSize, riffSize);
        } finally {
            fileChannel.position(finalPosition);
        }
    }

    private void writeLEInt(long value) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buf.putInt((int) value);
        buf.flip();
        int written = fileChannel.write(buf);
    }
}