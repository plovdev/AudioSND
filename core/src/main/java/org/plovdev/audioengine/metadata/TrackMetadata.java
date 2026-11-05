package org.plovdev.audioengine.metadata;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.TrackFormat;
import org.plovdev.audioengine.metadata.image.TrackImage;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Track metadata storage.
 *
 * @author Anton
 * @version 1.0
 * @see Track
 */
public class TrackMetadata {
    public static final TrackMetadata DEFAULT_METADATA = new TrackMetadata();

    static {
        DEFAULT_METADATA.setCopyright("PlovDev inc.");
        DEFAULT_METADATA.setCreationDate(new Date());
        DEFAULT_METADATA.setPublisher("AudioSND");
        DEFAULT_METADATA.setEncoder("AudioSND(loaders)");
    }

    private final Map<MetaKey, MetadataEntry> metadata = new ConcurrentHashMap<>();

    public TrackMetadata() {
    }

    private <T> void putMetadata(@NotNull MetaKey key, T value) {
        Class<?> type = key.getType();
        if (value != null) {
            if (!type.isInstance(value)) {
                throw new ClassCastException("Value " + value + " is not of pictureType " + type);
            }
        } // Allow null

        metadata.put(key, new MetadataEntry(value, type));
    }

    @SuppressWarnings("unchecked")
    private <T> T getMetadata(MetaKey key) {
        if (key == null) {
            return null;
        }
        Class<?> type = key.getType();
        MetadataEntry entry = metadata.get(key);
        if (entry == null) {
            return null;
        }
        if (!type.isAssignableFrom(entry.getType())) {
            throw new ClassCastException(String.format("Metadata %s is of pictureType %s, not %s", key, entry.getType().getSimpleName(), type.getSimpleName()));
        }
        return (T) entry.getValue();
    }
    // default getters

    public Optional<String> getTitle() {
        return Optional.ofNullable(getMetadata(MetaKey.TITLE));
    }

    public Optional<String> getArtist() {
        return Optional.ofNullable(getMetadata(MetaKey.ARTIST));
    }

    public Optional<String> getAlbum() {
        return Optional.ofNullable(getMetadata(MetaKey.ALBUM));
    }

    public Optional<String> getGenre() {
        return Optional.ofNullable(getMetadata(MetaKey.GENRE));
    }

    public Optional<Date> getYear() {
        return Optional.ofNullable(getMetadata(MetaKey.YEAR));
    }

    public Optional<Integer> getTrackNumber() {
        return Optional.ofNullable(getMetadata(MetaKey.TRACK_NUMBER));
    }

    public Optional<Duration> getDuration() {
        return Optional.ofNullable(getMetadata(MetaKey.DURATION));
    }

    public Optional<Integer> getSampleRate() {
        return Optional.ofNullable(getMetadata(MetaKey.SAMPLE_RATE));
    }

    public Optional<Integer> getBitDepth() {
        return Optional.ofNullable(getMetadata(MetaKey.BIT_DEPTH));
    }

    public Optional<Integer> getChannels() {
        return Optional.ofNullable(getMetadata(MetaKey.CHANNELS));
    }

    public Optional<String> getEncoder() {
        return Optional.ofNullable(getMetadata(MetaKey.ENCODER));
    }

    public Optional<String> getComment() {
        return Optional.ofNullable(getMetadata(MetaKey.COMMENT));
    }

    public Optional<String> getCopyright() {
        return Optional.ofNullable(getMetadata(MetaKey.COPYRIGHT));
    }

    public Optional<Path> getFilePath() {
        return Optional.ofNullable(getMetadata(MetaKey.FILE_PATH));
    }

    public Optional<Long> getFileSize() {
        return Optional.ofNullable(getMetadata(MetaKey.FILE_SIZE));
    }

    public Optional<String> getEncoding() {
        return Optional.ofNullable(getMetadata(MetaKey.ENCODING));
    }

    public Optional<Integer> getBitrate() {
        return Optional.ofNullable(getMetadata(MetaKey.BITRATE));
    }

    public Optional<Date> getCreationDate() {
        return Optional.ofNullable(getMetadata(MetaKey.CREATION_DATE));
    }

    public Optional<Date> getModificationDate() {
        return Optional.ofNullable(getMetadata(MetaKey.MODIFICATION_DATE));
    }

    public Optional<Float> getBpm() {
        return Optional.ofNullable(getMetadata(MetaKey.BPM));
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(getMetadata(MetaKey.KEY));
    }

    public Optional<String> getComposer() {
        return Optional.ofNullable(getMetadata(MetaKey.COMPOSER));
    }

    public Optional<String> getLyricist() {
        return Optional.ofNullable(getMetadata(MetaKey.LYRICIST));
    }

    public Optional<String> getPublisher() {
        return Optional.ofNullable(getMetadata(MetaKey.PUBLISHER));
    }

    public Optional<String> getIsrc() {
        return Optional.ofNullable(getMetadata(MetaKey.ISRC));
    }

    public Optional<Integer> getDiscNumber() {
        return Optional.ofNullable(getMetadata(MetaKey.DISC_NUMBER));
    }

    public Optional<Integer> getDiscTotal() {
        return Optional.ofNullable(getMetadata(MetaKey.DISC_TOTAL));
    }

    public Optional<Integer> getTrackTotal() {
        return Optional.ofNullable(getMetadata(MetaKey.TRACK_TOTAL));
    }

    public Optional<String> getLanguage() {
        return Optional.ofNullable(getMetadata(MetaKey.LANGUAGE));
    }

    public Optional<String> getMood() {
        return Optional.ofNullable(getMetadata(MetaKey.MOOD));
    }

    public Optional<String> getFileFormat() {
        return Optional.ofNullable(getMetadata(MetaKey.FILE_FORMAT));
    }

    public Optional<TrackFormat.AudioCodec> getAudioCodec() {
        return Optional.ofNullable(getMetadata(MetaKey.AUDIO_CODEC));
    }

    public Optional<TrackImage> getAlbumImage() {
        return Optional.ofNullable(getMetadata(MetaKey.ALBUM_ART));
    }

    public Optional<TrackImage> getArtistImage() {
        return Optional.ofNullable(getMetadata(MetaKey.ARTIST_IMAGE));
    }

    public Optional<String> getAlbumArtist() {
        return Optional.ofNullable(getMetadata(MetaKey.ALBUM_ARTIST));
    }

    // setters


    public void setTitle(String title) {
        putMetadata(MetaKey.TITLE, title);
    }

    public void setArtist(String artist) {
        putMetadata(MetaKey.ARTIST, artist);
    }

    public void setAlbum(String album) {
        putMetadata(MetaKey.ALBUM, album);
    }

    public void setGenre(String genre) {
        putMetadata(MetaKey.GENRE, genre);
    }

    public void setYear(Date year) {
        putMetadata(MetaKey.YEAR, year);
    }

    public void setTrackNumber(Integer trackNumber) {
        putMetadata(MetaKey.TRACK_NUMBER, trackNumber);
    }

    public void setDuration(Duration duration) {
        putMetadata(MetaKey.DURATION, duration);
    }

    public void setSampleRate(Integer sampleRate) {
        putMetadata(MetaKey.SAMPLE_RATE, sampleRate);
    }

    public void setBitDepth(Integer bitDepth) {
        putMetadata(MetaKey.BIT_DEPTH, bitDepth);
    }

    public void setChannels(Integer channels) {
        putMetadata(MetaKey.CHANNELS, channels);
    }

    public void setEncoder(String encoder) {
        putMetadata(MetaKey.ENCODER, encoder);
    }

    public void setComment(String comment) {
        putMetadata(MetaKey.COMMENT, comment);
    }

    public void setCopyright(String copyright) {
        putMetadata(MetaKey.COPYRIGHT, copyright);
    }

    public void setFilePath(Path filePath) {
        putMetadata(MetaKey.FILE_PATH, filePath);
    }

    public void setFileSize(Long fileSize) {
        putMetadata(MetaKey.FILE_SIZE, fileSize);
    }

    public void setEncoding(String encoding) {
        putMetadata(MetaKey.ENCODING, encoding);
    }

    public void setBitrate(Integer bitrate) {
        putMetadata(MetaKey.BITRATE, bitrate);
    }

    public void setCreationDate(Date creationDate) {
        putMetadata(MetaKey.CREATION_DATE, creationDate);
    }

    public void setModificationDate(Date modificationDate) {
        putMetadata(MetaKey.MODIFICATION_DATE, modificationDate);
    }

    public void setBpm(Float bpm) {
        putMetadata(MetaKey.BPM, bpm);
    }

    public void setKey(String key) {
        putMetadata(MetaKey.KEY, key);
    }

    public void setComposer(String composer) {
        putMetadata(MetaKey.COMPOSER, composer);
    }

    public void setLyricist(String lyricist) {
        putMetadata(MetaKey.LYRICIST, lyricist);
    }

    public void setPublisher(String publisher) {
        putMetadata(MetaKey.PUBLISHER, publisher);
    }

    public void setIsrc(String isrc) {
        putMetadata(MetaKey.ISRC, isrc);
    }

    public void setDiscNumber(Integer discNumber) {
        putMetadata(MetaKey.DISC_NUMBER, discNumber);
    }

    public void setDiscTotal(Integer discTotal) {
        putMetadata(MetaKey.DISC_TOTAL, discTotal);
    }

    public void setTrackTotal(Integer trackTotal) {
        putMetadata(MetaKey.TRACK_TOTAL, trackTotal);
    }

    public void setLanguage(String language) {
        putMetadata(MetaKey.LANGUAGE, language);
    }

    public void setMood(String mood) {
        putMetadata(MetaKey.MOOD, mood);
    }

    public void setFileFormat(String fileFormat) {
        putMetadata(MetaKey.FILE_FORMAT, fileFormat);
    }

    public void setAudioCodec(TrackFormat.AudioCodec audioCodec) {
        putMetadata(MetaKey.AUDIO_CODEC, audioCodec);
    }

    public void setAlbumImage(TrackImage image) {
        putMetadata(MetaKey.ALBUM_ART, image);
    }

    public void setArtistImage(TrackImage image) {
        putMetadata(MetaKey.ARTIST_IMAGE, image);
    }

    public void setAlbumArtist(String artist) {
        putMetadata(MetaKey.ALBUM_ARTIST, artist);
    }


    public int getNumData() {
        return metadata.size();
    }

    public Set<MetaKey> getKeys() {
        return metadata.keySet();
    }

    public Map<MetaKey, MetadataEntry> getAllTags() {
        return Map.copyOf(metadata);
    }

    public boolean isEmpty() {
        return metadata.isEmpty();
    }

    public void clear() {
        metadata.clear();
    }

    @Override
    public String toString() {
        return metadata.toString();
    }
}