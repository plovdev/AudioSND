package org.plovdev.audioengine.api.source;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AudioSource {
    private static final int WATERMARK = 3;
    private final Deque<ByteBuffer> buffersQueue;
    private final AudioProducer producer;
    private boolean isFinished;
    private final long limit;

    private final ByteBuffer EMPTY = ByteBuffer.allocateDirect(0);

    public AudioSource(@NonNull AudioProducer producer) {
        this.producer = producer;
        this.buffersQueue = new ConcurrentLinkedDeque<>();
        this.limit = producer.limit();
        initQueue();
    }

    public void initQueue() {
        int available = producer.loadNextChunk(buffersQueue);
        if (available <= 0) {
            isFinished = true;
        } else {
            waitLoaded();
        }
    }

    public ByteBuffer nextChunk() {
        ByteBuffer ready = buffersQueue.poll();
        if (ready != null) {
            requestMoreIfLow();
            return ready;
        }
        if (isFinished) return null; // return null if finish

        int available = producer.loadNextChunk(buffersQueue);
        if (available <= 0) {
            isFinished = true;
            return buffersQueue.poll();
        }

        if (buffersQueue.isEmpty()) {
            waitLoaded();
        }

        return buffersQueue.poll();
    }

    private void waitLoaded() {
        try {
            producer.awaitChunkLoaded();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void requestMoreIfLow() {
        if (isFinished) return;
        if (buffersQueue.size() >= WATERMARK) return;

        int available = producer.loadNextChunk(buffersQueue);
        if (available <= 0) {
            isFinished = true;
        }
    }

    public boolean isFinished() {
        return isFinished && buffersQueue.isEmpty();
    }

    public long getLimit() {
        return limit;
    }
}