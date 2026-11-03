package org.plovdev.audioengine.loaders.raw;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.*;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.plovdev.audioengine.loaders.ExportUtils.getFile;

public class RawTrackLoader implements TrackLoader {
    private static final Logger log = LoggerFactory.getLogger(RawTrackLoader.class);
    private final List<PathLocator> locators = new CopyOnWriteArrayList<>();
    private LoadListener loadListener = new LoadListenerAdapter() {};
    private final TrackFormat format;

    public RawTrackLoader(TrackFormat format) {
        this.format = format;
    }

    public void addLoactor(PathLocator locator) {
        locators.add(locator);
    }

    @Override
    public Track loadTrack(@NonNull File base) throws TrackLoadException {
        log.debug("Loading file: {}", base.getName());
        File file = getFile(base, locators);
        try (InputStream stream = new FileInputStream(file)) {
            return loadTrack(stream);
        } catch (Exception e) {
            throw new TrackLoadException("Failed to load RAW file: " + file.getName() + " - " + e);
        }
    }

    @Override
    public Track loadTrack(InputStream stream) throws TrackLoadException {
        ByteBuffer data;
        try (stream) {
            try {
                int bytesRead = 0;
                int chunkSize = 8192; // 8KB chunks

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[chunkSize];

                if (loadListener != null) {
                    loadListener.onLoadStarted(-1);
                }

                int n;
                while ((n = stream.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                    bytesRead += n;
                }

                byte[] allBytes = baos.toByteArray();
                data = ByteBuffer.allocateDirect(allBytes.length);
                data.order(format.byteOrder());
                data.put(allBytes);
                data.flip();

                log.debug("File loaded successful: {} bytes", bytesRead);

                if (loadListener != null) {
                    loadListener.onLoadFinished();
                }

                int bytesPerSample = format.bitDepth() / 8;
                int bytesPerFrame = format.channels() * bytesPerSample;
                long totalFrames = allBytes.length / bytesPerFrame;
                Duration duration = ExportUtils.calculateDuration(totalFrames, format.sampleRate());

                TrackMetadata metadata = getTrackMetadata(duration);
                metadata.setFileSize((long) allBytes.length);

                //TODO: decode to float32
                return new Track(data, duration, format.toAudioFormat(), metadata);

            } catch (TrackLoadException e) {
                if (loadListener != null) {
                    loadListener.onLoadFailed(e);
                }
                throw new TrackLoadException("Failed to load RAW from stream: " + e.getMessage());
            }
        } catch (IOException e) { throw new TrackLoadException("Cann't load RAW: " + e.getMessage()); }
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
    public Track loadTrack(@NonNull URI uri) throws TrackLoadException {
        return switch (uri.getScheme()) {
            case "file" -> loadTrack(new File(uri.toString()));
            case "https", "http" -> {
                try (InputStream stream = uri.toURL().openStream()) {
                    yield loadTrack(stream);
                } catch (Exception e) {
                    throw new TrackLoadException(e.getMessage());
                }
            }
            default -> throw new TrackLoadException("Unsupported URI scheme: " + uri.getScheme());
        };
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
    public TrackMetadata readTrackMetadata(URI src) {
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
    public TrackFormat getTrackFormat(URI src) {
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
