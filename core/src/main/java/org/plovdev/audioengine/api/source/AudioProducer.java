package org.plovdev.audioengine.api.source;

import java.nio.ByteBuffer;
import java.util.Deque;

public interface AudioProducer {
    void setChunkSize(int size);

    /**
     * Load next chunk in queue.
     *
     * @param queueToLoad queue in loader will load chunk
     * @return available bytes
     */
    int loadNextChunk(Deque<ByteBuffer> queueToLoad);

    void awaitChunkLoaded();

    void setPointer(long position);

    long limit();
}