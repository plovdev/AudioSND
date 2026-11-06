package org.plovdev.audioengine.loaders.wav;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.plovdev.audioengine.api.source.AudioSource;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.InputStreamAudioProducer;
import org.plovdev.audioengine.loaders.LoadListener;
import org.plovdev.audioengine.loaders.TrackLoader;
import org.plovdev.audioengine.loaders.format.TrackFormatUtils;
import org.plovdev.audioengine.loaders.wav.chunks.FormatChunk;
import org.plovdev.audioengine.loaders.wav.chunks.ListChunk;
import org.plovdev.audioengine.loaders.wav.chunks.TagEntry;
import org.plovdev.audioengine.loaders.wav.read.WavParser;
import org.plovdev.audioengine.loaders.wav.read.parsers.APICParser;
import org.plovdev.audioengine.loaders.wav.struct.Chunk;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class WavTrackLoader implements TrackLoader {
    private static final Logger log = LoggerFactory.getLogger(WavTrackLoader.class);
    private LoadListener loadListener = new LoadListener() {
    };

    @Override
    public Track loadTrack(@NonNull File file) throws TrackLoadException {
        log.debug("Loading file: {}", file.getName());
        try (InputStream stream = new FileInputStream(file)) {
            return loadTrack(stream, file.length());
        } catch (Exception e) {
            throw new TrackLoadException("Failed to load WAV file: " + file.getName() + " - " + e);
        }
    }

    @Override
    public @NotNull Track loadTrack(@NotNull InputStream stream) {
        return loadTrack(stream, -1);
    }

    public Track loadTrack(InputStream stream, long total) throws TrackLoadException {
        try {
            WavParser parser = new WavParser(stream);
            parser.parse();
            log.debug("File parsing finished, collect result...");

            FormatChunk formatChunk = parser.getFormatChunk();
            TrackFormat format = formatChunk.getFormat();
            ListChunk listChunk = parser.getListChunk();
            Duration duration = Duration.ofMillis(TrackFormatUtils.calculateDurationMs(format, total));

            TrackMetadata metadata = new TrackMetadata();
            metadata.setDuration(duration);
            metadata.setChannels(format.channels());
            metadata.setBitrate(format.bitRate());
            metadata.setBitDepth(format.bitDepth());
            metadata.setSampleRate(format.sampleRate());
            metadata.setAudioCodec(format.audioCodec());

            if (listChunk != null) {
                for (Chunk tag : listChunk.getEntries()) {
                    TagEntry entry = (TagEntry) tag;
                    String content = entry.getContent();
                    switch (tag.getChunk()) {
                        case INAM, TIT2 -> metadata.setTitle(content);
                        case ISFT, TPUB -> metadata.setPublisher(content);
                        case ICRD, TDRC, TYER -> metadata.setYear(parsePartialDate(content).getTime());
                        case TDRL -> metadata.setCreationDate(parsePartialDate(content).getTime());
                        case TBPM -> metadata.setBpm(safeParseFloat(content));
                        case IPRD, TALB -> metadata.setAlbum(content);
                        case IART, TPE1 -> metadata.setArtist(content);
                        case IGNR, TCON -> metadata.setGenre(content);
                        case ITRK, TRCK -> {
                            try {
                                metadata.setTrackNumber(Integer.parseInt(content));
                            } catch (Exception e) {
                                if (content.contains("/")) {
                                    String first = content.substring(0, content.indexOf("/"));
                                    String last = content.substring(content.indexOf("/") + 1);

                                    metadata.setTrackNumber(safeParseTrackNumber(first));
                                    metadata.setTrackTotal(safeParseTrackNumber(last));
                                }
                            }
                        }
                        case ICMT, COMM -> metadata.setComment(content);
                        case ICOP, TCOP -> metadata.setCopyright(content);
                        case TLAN -> metadata.setLanguage(content);
                        case TPE2 -> metadata.setAlbumArtist(content);
                        case TCOM -> metadata.setComposer(content);
                        case TKEY -> metadata.setKey(content);
                        case TMOO -> metadata.setMood(content);
                        case TPOS -> {
                            try {
                                metadata.setDiscNumber(Integer.parseInt(content));
                            } catch (Exception e) {
                                if (content.contains("/")) {
                                    String first = content.substring(0, content.indexOf("/"));
                                    String last = content.substring(content.indexOf("/") + 1);

                                    metadata.setDiscNumber(safeParseTrackNumber(first));
                                    metadata.setDiscTotal(safeParseTrackNumber(last));
                                }
                            }
                        }
                        case ISRC -> metadata.setIsrc(content);
                        case APIC -> metadata.setAlbumImage(APICParser.parseToImage(entry.getBody()));
                    }
                }
            }

            log.debug("File loaded successful");
            if (loadListener != null) {
                loadListener.onLoadFinished();
            }

            return new Track(new AudioSource(new InputStreamAudioProducer(stream, format, total)), duration, format.toAudioFormat(), metadata);
        } catch (Exception e) {
            if (loadListener != null) {
                loadListener.onLoadFailed(new TrackLoadException(e));
            }
            throw new TrackLoadException("Failed to load WAV from stream: " + e);
        }
    }

    @Override
    public TrackMetadata readTrackMetadata(File src) {
        return null;
    }

    @Override
    public TrackMetadata readTrackMetadata(InputStream src) {
        return null;
    }

    @Override
    public TrackFormat getTrackFormat(File src) {
        return null;
    }

    @Override
    public TrackFormat getTrackFormat(InputStream src) {
        return null;
    }

    private @NonNull Calendar parsePartialDate(@NonNull String input) {
        String[] parts = input.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = (parts.length > 1) ? Integer.parseInt(parts[1]) - 1 : 0; // Месяц 0–11
        int day = (parts.length > 2) ? Integer.parseInt(parts[2]) : 1;

        Calendar calendar = new GregorianCalendar();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    @Contract(pure = true)
    private @Nullable Integer safeParseTrackNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void setLoadListener(LoadListener listener) {
        loadListener = listener;
    }

    @Override
    public LoadListener getLoadListener() {
        return loadListener;
    }

    @Contract(pure = true)
    private static @Nullable Float safeParseFloat(String floatStr) {
        try {
            return Float.parseFloat(floatStr);
        } catch (Exception e) {
            return null;
        }
    }
}