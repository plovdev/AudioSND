package org.plovdev.audioengine.loaders.raw;

import org.plovdev.audioengine.loaders.*;
import org.plovdev.audioengine.format.TrackFormat;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.plovdev.audioengine.format.TrackFormat.AudioCodec.FLOAT32;
import static org.plovdev.audioengine.format.TrackFormat.AudioCodec.FLOAT64;
import static org.plovdev.audioengine.format.TrackFormat.AudioCodec.PCM32;

public class RawTrackLoaderManager implements TrackLoaderManager {
    private final RawTrackLoader loader;
    private final RawTrackExporter exporter;
    private static final List<TrackFormat.AudioCodec> supportedCodecs = List.of(PCM8, PCM16, PCM24, PCM32, FLOAT32, FLOAT64);

    public RawTrackLoaderManager(TrackFormat format) {
        loader = new RawTrackLoader(format);
        exporter = new RawTrackExporter();
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
        throw new UnsupportedOperationException("Raw not supports encoder");
    }

    @Override
    public TrackDecoder getTrackDecoder() {
        throw new UnsupportedOperationException("Raw not supports decoder");
    }

    @Override
    public boolean isSupported(File file) {
        if (file == null) return false;
        String filename = file.getName();

        String lower = filename.toLowerCase().trim();
        lower = lower.startsWith(".") ? lower : "." + lower;
        return lower.endsWith(".raw");
    }

    @Override
    public boolean isSupported(TrackFormat format) {
        TrackFormat.AudioCodec codec = format.audioCodec();
        return supportedCodecs.contains(codec);
    }

    @Override
    public boolean isSupported(InputStream stream) {
        throw new UnsupportedOperationException("Cann't check supports in RAW");
    }

    @Override
    public boolean isSupported(URI uri) {
        return isSupported(new File(uri.getPath()));
    }

    @Override
    public void registerPathLocator(PathLocator locator) {
        loader.addLoactor(locator);
    }
}