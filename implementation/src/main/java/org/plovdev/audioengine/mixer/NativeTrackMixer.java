package org.plovdev.audioengine.mixer;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.exceptions.MixingException;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.format.AudioFormatFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NativeTrackMixer implements TrackMixer {
    private AudioFormat outputFormat = AudioFormatFactory.stereo44100();
    private final List<Track> mixingTracks = new CopyOnWriteArrayList<>();

    /**
     * Setup output track format(after mixing)
     *
     * @param format output format
     */
    @Override
    public void setOutputFormat(AudioFormat format) {
        outputFormat = format;
    }

    /**
     * Get setuped output track format
     */
    @Override
    public AudioFormat getOutputFormat() {
        return outputFormat;
    }

    @Override
    public void addTrack(Track track) {
        mixingTracks.add(track);
    }

    @Override
    public void removeTrack(Track track) {
        mixingTracks.remove(track);
    }

    /**
     * Get all tracks, which will be mixing.
     *
     * @return mixing tracks
     */
    @Override
    public List<Track> getMixingTracks() {
        return mixingTracks;
    }

    /**
     * Clear tracks from mixer
     */
    @Override
    public void clearTracks() {
        mixingTracks.clear();
    }

    /**
     * Mix all track in list.
     *
     * @throws MixingException when mixing failed.
     */
    @Override
    public Track doMixing() throws MixingException {
        if (isEmpty()) {
            throw new MixingException("No tracks for mixing.");
        }
        if (mixingTracks.size() == 1) {
            return mixingTracks.getFirst();
        }
        return _doMixing(mixingTracks, outputFormat);
    }

    @Override
    public boolean isEmpty() {
        return mixingTracks.isEmpty();
    }

    private native Track _doMixing(List<Track> tracks, AudioFormat format);
}