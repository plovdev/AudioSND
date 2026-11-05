package org.plovdev.audioengine.api.source;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class AudioSource {
    private final Deque<ByteBuffer> buffersQueue;
    private final AudioProducer producer;
    private boolean isFinished;
    private final long limit;

    private final ByteBuffer emptyFallback = ByteBuffer.allocateDirect(0);

    public AudioSource(@NonNull AudioProducer producer) {
        this.producer = producer;
        this.buffersQueue = new ConcurrentLinkedDeque<>();
        this.limit = producer.limit();
    }

    public void initQueue() {
        int available = producer.loadNextChunk(buffersQueue);
        if (available <= 0) {
            isFinished = true;
        } else {
            producer.awaitChunkLoaded();
        }
    }

    public ByteBuffer nextChunk() {
        if (isFinished && buffersQueue.isEmpty()) {
            return emptyFallback;
        }

        if (isFinished) {
            return buffersQueue.poll();
        }

        int available = producer.loadNextChunk(buffersQueue);
        if (available <= 0) {
            isFinished = true;
            ByteBuffer remaining = buffersQueue.poll();
            return remaining != null ? remaining : emptyFallback;
        }

        if (buffersQueue.isEmpty()) {
            producer.awaitChunkLoaded();
        }

        ByteBuffer current = buffersQueue.poll();
        return current != null ? current : emptyFallback;
    }

    public boolean isFinished() {
        return isFinished && buffersQueue.isEmpty();
    }

    public long getLimit() {
        return limit;
    }
}