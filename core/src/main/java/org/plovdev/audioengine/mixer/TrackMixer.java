package org.plovdev.audioengine.mixer;

import org.plovdev.audioengine.api.Track;
import org.plovdev.audioengine.exceptions.MixingException;
import org.plovdev.audioengine.format.AudioFormat;

import java.util.List;

/**
 * Tracks mixing manager.
 *
 * @author Anton
 * @version 1.0
 */
public interface TrackMixer {
    /**
     * Setup output track format(after mixing)
     */
    void setOutputFormat(AudioFormat format);

    /**
     * Get configured output track format
     */
    AudioFormat getOutputFormat();


    /**
     * Adds a track to the mixing list
     *
     * @param track to mix
     */
    void addTrack(Track track);

    /**
     * Removes a track from the mixing list
     *
     * @param track track to mix
     */
    void removeTrack(Track track);

    /**
     * Get all tracks, which will be mixing.
     *
     * @return mixing tracks
     */
    List<Track> getMixingTracks();

    /**
     * Clear tracks from mixer
     */
    void clearTracks();

    /**
     * Mixes all added tracks into a single new track.
     * <p>
     * Creates a new track instance. Original tracks are not modified.
     * Output format must be set via {@link #setOutputFormat(AudioFormat)}
     * before calling this method.
     * </p>
     *
     * @return new mixed track
     * @throws MixingException if mixing failed
     */
    Track doMixing();


    boolean isEmpty();

    /**
     * Returns tracks in mixer.
     *
     * @return mixing tracks.
     */
    default int getTrackCount() {
        return getMixingTracks().size();
    }
}