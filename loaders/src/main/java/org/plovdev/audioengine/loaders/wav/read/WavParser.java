package org.plovdev.audioengine.loaders.wav.read;

import org.plovdev.audioengine.loaders.wav.chunks.FormatChunk;
import org.plovdev.audioengine.loaders.wav.chunks.ListChunk;
import org.plovdev.audioengine.loaders.wav.struct.Chunk;
import org.plovdev.audioengine.loaders.wav.struct.WavChunkId;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WavParser {
    private final List<Chunk> chunks = new CopyOnWriteArrayList<>();
    private final WavChunkReader chunkReader;

    public WavParser(InputStream stream) {
        chunkReader = new WavChunkReader(stream);
    }

    public void parse() throws IOException {
        chunkReader.validateRiffHeader(); // проверка RIFF/WAVE

        Chunk chunk;
        while ((chunk = chunkReader.readNextChunk()) != null) {
            chunks.add(chunk);
        }
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public FormatChunk getFormatChunk() {
        for (Chunk chunk : getChunks()) {
            if (chunk instanceof FormatChunk) {
                return (FormatChunk) chunk;
            }
        }
        return null;
    }

    public ListChunk getListChunk() {
        ListChunk totalChunk = new ListChunk(WavChunkId.LIST, new ArrayList<>());
        for (Chunk chunk : getChunks()) {
            if (chunk instanceof ListChunk listChunk) {
                totalChunk.getEntries().addAll(listChunk.getEntries());
            }
        }
        return totalChunk;
    }

    public WavChunkReader getChunkReader() {
        return chunkReader;
    }
}