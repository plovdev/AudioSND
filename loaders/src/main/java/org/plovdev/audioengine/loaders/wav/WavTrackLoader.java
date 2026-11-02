package org.plovdev.audioengine.loaders.wav;

import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.loaders.format.TrackFormatUtils;
import org.plovdev.audioengine.loaders.LoadListener;
import org.plovdev.audioengine.loaders.LoadListenerAdapter;
import org.plovdev.audioengine.loaders.PathLocator;
import org.plovdev.audioengine.loaders.TrackLoader;
import org.plovdev.audioengine.loaders.wav.chunks.DataChunk;
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
import java.net.URI;
import java.time.Duration;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.plovdev.audioengine.loaders.ExportUtils.getFile;

public class WavTrackLoader implements TrackLoader {
    private static final Logger log = LoggerFactory.getLogger(WavTrackLoader.class);
    private final List<PathLocator> locators = new CopyOnWriteArrayList<>();
    private LoadListener loadListener = new LoadListenerAdapter() {
    };

    public void addLoactor(PathLocator locator) {
        locators.add(locator);
    }

    @Override
    public Track loadTrack(File base) throws TrackLoadException {
        log.debug("Loading file: {}", base.getName());
        File file = getFile(base, locators);
        try (InputStream stream = new FileInputStream(file)) {
            return loadTrack(stream);
        } catch (Exception e) {
            throw new TrackLoadException("Failed to load WAV file: " + file.getName() + " - " + e);
        }
    }

    @Override
    public Track loadTrack(InputStream stream) throws TrackLoadException {
        try {
            int total = stream.available();
            if (loadListener != null) {
                loadListener.onLoadStarted(total);
            }
            WavParser parser = new WavParser(stream);
            parser.parse();
            log.debug("File parsing finished, collect result...");

            DataChunk chunk = parser.getDataChunk();
            FormatChunk formatChunk = parser.getFormatChunk();
            TrackFormat format = formatChunk.getFormat();

            ListChunk listChunk = parser.getListChunk();

            Duration duration = Duration.ofMillis(TrackFormatUtils.calculateDurationMs(format, chunk.getSize()));

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
                        case TBPM -> metadata.setBpm(safeParseFloar(content));
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
            return new Track(chunk.getData().order(format.byteOrder()), duration, format, metadata);
        } catch (Exception e) {
            if (loadListener != null) {
                loadListener.onLoadFailed(new TrackLoadException(e));
            }
            throw new TrackLoadException("Failed to load WAV from stream: " + e);
        }
    }

    @Override
    public Track loadTrack(URI uri) throws TrackLoadException {
        try (InputStream stream = uri.toURL().openStream()) {
            return loadTrack(stream);
        } catch (Exception e) {
            throw new TrackLoadException(e.getMessage());
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
    public TrackMetadata readTrackMetadata(URI src) {
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

    @Override
    public TrackFormat getTrackFormat(URI src) {
        return null;
    }

    private Calendar parsePartialDate(String input) {
        String[] parts = input.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = (parts.length > 1) ? Integer.parseInt(parts[1]) - 1 : 0; // Месяц 0–11
        int day = (parts.length > 2) ? Integer.parseInt(parts[2]) : 1;


        Calendar calendar = new GregorianCalendar();
        calendar.set(year, month, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Integer safeParseTrackNumber(String number) {
        try {
            return Integer.parseInt(number);
        } catch (Exception e) {
            return null;
        }
    }

    private Float safeParseFloar(String number) {
        try {
            return Float.parseFloat(number);
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
}