package org.plovdev.audioengine.loaders;

import org.jspecify.annotations.Nullable;
import org.plovdev.audioengine.api.source.AudioProducer;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.wav.decoders.CoderUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InputStreamAudioProducer implements AudioProducer, AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio-producer");
        t.setDaemon(true);
        return t;
    });

    private final Semaphore loadRequests = new Semaphore(0, true);
    private final Semaphore chunksReady = new Semaphore(0, true);
    private final TrackFormat trackFormat;
    private final AtomicInteger chunkSize = new AtomicInteger(4096);
    private final InputStream readStream;
    private final long limit;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean eof;

    public InputStreamAudioProducer(InputStream readStream, TrackFormat format, long limit) {
        this.readStream = readStream;
        this.limit = limit;
        this.trackFormat = format;
    }

    @Override
    public void setChunkSize(int size) {
        chunkSize.set(size);
    }

    @Override
    public int loadNextChunk(Deque<ByteBuffer> queue) {
        if (started.compareAndSet(false, true)) {
            executor.submit(() -> pump(queue));
        }
        if (eof) return 0;
        loadRequests.release();
        return 1;
    }

    private void pump(Deque<ByteBuffer> queue) {
        try {
            while (!eof) {
                loadRequests.acquire();
                if (eof) break;

                ByteBuffer buf = loadNext();
                if (buf == null) {
                    eof = true;
                    break;
                }
                queue.add(buf);
                chunksReady.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            eof = true;
            throw new RuntimeException(t);
        } finally {
            chunksReady.release();
        }
    }

    private @Nullable ByteBuffer loadNext() throws IOException {
        int size = chunkSize.get();
        byte[] buffer = readStream.readNBytes(size);
        if (buffer.length == 0) return null;

        ByteBuffer chunk = ByteBuffer.allocateDirect(buffer.length).order(trackFormat.byteOrder());
        chunk.put(buffer);
        chunk.flip();

        return CoderUtils.decodeToFloat32(chunk, trackFormat.audioCodec());
    }

    @Override
    public void awaitChunkLoaded() throws InterruptedException {
        chunksReady.acquire();
    }

    @Override
    public void setPointer(long position) { /* TODO */ }

    @Override
    public long limit() {
        return limit;
    }

    @Override
    public void close() {
        eof = true;
        loadRequests.release();
        executor.shutdownNow();
        try {
            readStream.close();
        } catch (IOException ignored) {
        }
    }
}