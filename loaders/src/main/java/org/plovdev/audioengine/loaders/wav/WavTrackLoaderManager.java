package org.plovdev.audioengine.loaders.wav;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.*;
import org.plovdev.audioengine.loaders.wav.write.WavTrackExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.plovdev.audioengine.format.TrackFormat.AudioCodec.*;

public class WavTrackLoaderManager implements TrackLoaderManager {
    private static final Logger log = LoggerFactory.getLogger(WavTrackLoaderManager.class);
    private static final List<TrackFormat.AudioCodec> supportedCodecs = List.of(PCM8, PCM16, PCM24, PCM32, FLOAT32, FLOAT64, ALAW, ULAW, IMA_ADPCM, MIC_ADPCM);

    private final WavTrackLoader loader;
    private final WavTrackExporter exporter;
    private final WavTrackEncoder encoder;
    private final WavTrackDecoder decoder;

    public WavTrackLoaderManager() {
        loader = new WavTrackLoader();
        exporter = new WavTrackExporter();
        encoder = new WavTrackEncoder();
        decoder = new WavTrackDecoder();
    }

    @Override
    public TrackLoader getTrackLoader() {
        return loader;
    }

    @Override
    public TrackExporter getTrackExporter() {
        return exporter;
    }

    @Override
    public TrackEncoder getTrackEncoder() {
        return encoder;
    }

    @Override
    public TrackDecoder getTrackDecoder() {
        return decoder;
    }

    @Override
    public boolean isSupported(File file) {
        if (file == null) return false;
        String filename = file.getName();

        String lower = filename.toLowerCase().trim();
        lower = lower.startsWith(".") ? lower : "." + lower;
        return lower.endsWith(".wav") || lower.endsWith(".wave");
    }

    @Override
    public boolean isSupported(@NonNull TrackFormat format) {
        TrackFormat.AudioCodec codec = format.audioCodec();
        return supportedCodecs.contains(codec);
    }

    @Override
    public boolean isSupported(InputStream stream) {
        byte[] header = new byte[12];

        try {
            if (stream.markSupported()) {
                stream.mark(12);
            }

            int totalRead = 0;
            while (totalRead < 12) {
                int read = stream.read(header, totalRead, 12 - totalRead);
                if (read == -1) {
                    return false;
                }
                totalRead += read;
            }

            String riff = new String(header, 0, 4, StandardCharsets.US_ASCII);
            if (!riff.equals("RIFF")) {
                log.info("File is not RIFF based! {}", riff);
                return false;
            }

            String wave = new String(header, 8, 4, StandardCharsets.US_ASCII);
            if (!wave.equals("WAVE")) {
                log.info("Not WAVE file! {}", wave);
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (stream.markSupported()) {
                    stream.reset();
                }
            } catch (IOException e) {
                log.warn("Не удалось сбросить поток", e);
            }
        }
    }

    @Override
    public boolean isSupported(@NonNull URI uri) {
        return isSupported(new File(uri.getPath()));
    }

    @Override
    public void registerPathLocator(PathLocator locator) {
        loader.addLoactor(locator);
    }
}