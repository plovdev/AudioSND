package org.plovdev.audioengine.loaders.raw;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.AudioSource;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.ExportUtils;
import org.plovdev.audioengine.loaders.InputStreamAudioProducer;
import org.plovdev.audioengine.loaders.LoadListener;
import org.plovdev.audioengine.loaders.TrackLoader;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;

public class RawTrackLoader implements TrackLoader {
    private static final Logger log = LoggerFactory.getLogger(RawTrackLoader.class);
    private LoadListener loadListener = new LoadListener() {
    };

    private final TrackFormat format;

    public RawTrackLoader(TrackFormat format) {
        this.format = format;
    }

    @Override
    public Track loadTrack(@NonNull File file) throws TrackLoadException {
        log.debug("Loading file: {}", file.getName());
        try (InputStream stream = new FileInputStream(file)) {
            return loadTrack(stream, file.length());
        } catch (Exception e) {
            throw new TrackLoadException("Failed to load RAW file: " + file.getName() + " - " + e);
        }
    }

    @Override
    public @NotNull Track loadTrack(@NotNull InputStream stream) {
        return loadTrack(stream, -1);
    }

    public Track loadTrack(InputStream stream, long totalBytes) throws TrackLoadException {
        int bytesPerSample = format.bitDepth() / 8;
        int bytesPerFrame = format.channels() * bytesPerSample;
        long totalFrames = totalBytes / bytesPerFrame;
        Duration duration = ExportUtils.calculateDuration(totalFrames, format.sampleRate());

        TrackMetadata metadata = getTrackMetadata(duration);
        metadata.setFileSize(totalBytes);

        return new Track(new AudioSource(new InputStreamAudioProducer(stream, format, totalBytes)), duration, format.toAudioFormat(), metadata);
    }

    private @NonNull TrackMetadata getTrackMetadata(Duration duration) {
        TrackMetadata metadata = new TrackMetadata();
        metadata.setDuration(duration);
        metadata.setChannels(format.channels());
        metadata.setAudioCodec(format.audioCodec());
        metadata.setEncoding(format.audioCodec().name());
        metadata.setSampleRate(format.sampleRate());
        metadata.setBitDepth(format.bitDepth());
        metadata.setBitrate(format.bitRate());

        return metadata;
    }

    @Override
    public TrackMetadata readTrackMetadata(File src) {
        throw new UnsupportedOperationException("Raw not supports metadata");
    }

    @Override
    public TrackMetadata readTrackMetadata(InputStream src) {
        throw new UnsupportedOperationException("Raw not supports metadata");
    }

    @Override
    public TrackFormat getTrackFormat(File src) {
        throw new UnsupportedOperationException("Raw not supports format");
    }

    @Override
    public TrackFormat getTrackFormat(InputStream src) {
        throw new UnsupportedOperationException("Raw not supports format");
    }

    @Override
    public void setLoadListener(LoadListener listener) {
        loadListener = listener;
    }

    @Override
    public LoadListener getLoadListener() {
        return loadListener;
    }
}