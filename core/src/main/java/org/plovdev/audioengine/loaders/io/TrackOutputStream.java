package org.plovdev.audioengine.loaders.io;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public abstract class TrackOutputStream extends OutputStream {
    private static final Logger log = LoggerFactory.getLogger(TrackOutputStream.class);
    protected final TrackFormat trackFormat;
    protected TrackMetadata trackMetadata;
    protected final OutputStream toWrite;

    public TrackOutputStream(TrackFormat trackFormat, OutputStream toWrite) {
        this.trackFormat = trackFormat;
        this.toWrite = toWrite;
    }

    public TrackOutputStream(TrackFormat trackFormat, TrackMetadata metadata, OutputStream toWrite) {
        this.trackFormat = trackFormat;
        this.trackMetadata = metadata;
        this.toWrite = toWrite;
    }

    public abstract void writeHead() throws IOException;

    protected abstract void writeEnd() throws IOException;

    @Override
    public synchronized void write(int b) throws IOException {
        toWrite.write(b);
    }

    @Override
    public synchronized void write(@NotNull byte[] b) throws IOException {
        toWrite.write(b);
    }

    @Override
    public synchronized void write(@NotNull byte[] b, int off, int len) throws IOException {
        toWrite.write(b, off, len);
    }

    public synchronized void write(ByteBuffer buffer) throws IOException {
        if (buffer == null) {
            log.warn("Buffer is null!");
            return;
        }
        buffer.rewind();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        this.write(bytes);
    }

    @Override
    public synchronized void close() throws IOException {
        writeEnd();
        toWrite.close();
    }
}