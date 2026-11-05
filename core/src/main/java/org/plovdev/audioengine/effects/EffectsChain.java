package org.plovdev.audioengine.effects;

import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.format.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EffectsChain {
    private final List<AudioEffect> effects = new ArrayList<>();

    public EffectsChain() {
    }

    public EffectsChain addEffect(AudioEffect effect) {
        Objects.requireNonNull(effect);
        effects.add(effect);
        return this;
    }

    public EffectsChain removeEffect(AudioEffect effect) {
        Objects.requireNonNull(effect);
        effects.remove(effect);
        return this;
    }

    /**
     * Applies the chain of effects to a track.
     * <p>
     * The track's audio data is read-only. Effects that need mutable buffers
     * must copy the data internally.
     * </p>
     * <p>
     * This method modifies neither the original track nor its buffer.
     * Returns a new track instance with processed audio.
     * </p>
     *
     * @param source input track (not modified)
     * @return new track with applied effects
     * @throws IllegalArgumentException if source is null
     */
    public Track apply(Track source) {
//        if (source == null) {
//            throw new IllegalArgumentException("Source track cannot be null");
//        }
//        if (effects.isEmpty()) {
//            return source;
//        }
//
//        AudioFormat format = source.getFormat();
//
//        ByteBuffer original = source.getTrackData();
//        original.rewind();
//        ByteBuffer copy = ByteBuffer.allocateDirect(original.capacity());
//        copy.order(ByteOrder.LITTLE_ENDIAN);
//        copy.put(original);
//        copy.rewind();
//
//        for (AudioEffect effect : effects) {
//            effect.processInPlace(copy);
//            copy.rewind();
//        }
//
//        return new Track(copy, source.getDuration(), format, source.getMetaData());
        return null;
    }
}