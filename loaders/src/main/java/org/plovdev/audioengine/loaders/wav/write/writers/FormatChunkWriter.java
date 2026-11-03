package org.plovdev.audioengine.loaders.wav.write.writers;

import org.plovdev.audioengine.exceptions.loaders.TrackExportException;
import org.plovdev.audioengine.format.TrackFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

import static org.plovdev.audioengine.loaders.ExportUtils.intToLittleEndian;
import static org.plovdev.audioengine.loaders.ExportUtils.shortToLittleEndian;
import static org.plovdev.audioengine.loaders.wav.struct.WavChunkId.FMT;

public class FormatChunkWriter implements WavChunkWriter<TrackFormat> {
    private static final Logger log = LoggerFactory.getLogger(FormatChunkWriter.class);

    @Override
    public Class<?> getType() {
        return TrackFormat.class;
    }

    @Override
    public void write(OutputStream outputStream, TrackFormat outputFormat, TrackFormat format) {
        try {
            int channels = format.channels();
            int sampleRate = format.sampleRate();
            int bitDepth = format.bitDepth();

            int blockAlign = channels * (bitDepth / 8);
            int byteRate = sampleRate * blockAlign;

            outputStream.write(FMT.getChunk().getBytes());
            outputStream.write(intToLittleEndian(16));

            switch (format.audioCodec()) {
                case PCM8, PCM16, PCM24, PCM32 -> outputStream.write(shortToLittleEndian((short) 1));
                case IMA_ADPCM -> outputStream.write(shortToLittleEndian((short) 2));
                case FLOAT32, FLOAT64 -> outputStream.write(shortToLittleEndian((short) 3));
                case ALAW -> outputStream.write(shortToLittleEndian((short) 4));
                case ULAW -> outputStream.write(shortToLittleEndian((short) 5));
                default -> throw new IllegalArgumentException("Unsupported format codec");
            }

            outputStream.write(shortToLittleEndian((short) channels));
            outputStream.write(intToLittleEndian(sampleRate));
            outputStream.write(intToLittleEndian(byteRate));
            outputStream.write(shortToLittleEndian((short) blockAlign));
            outputStream.write(shortToLittleEndian((short) bitDepth));
        } catch (Exception e) {
            throw new TrackExportException(e.getMessage());
        }
    }
}