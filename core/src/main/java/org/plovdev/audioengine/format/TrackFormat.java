package org.plovdev.audioengine.format;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.nio.ByteOrder;

/**
 * Track format description for clients.
 *
 * @param channels   channels count in audio.
 * @param bitDepth   audio format width.
 * @param sampleRate audio sample rate.
 * @param signed     is signed audio.
 * @param byteOrder  audio byte order.
 * @param audioCodec audio data codec.
 * @author Anton
 * @version 1.0
 */

public record TrackFormat(int channels, int bitDepth, int sampleRate, boolean signed, @NotNull ByteOrder byteOrder,
                          @NotNull AudioCodec audioCodec) {
    /**
     * Audio codec before converted to pcm
     */
    public enum AudioCodec {
        PCM8,       // 8-bit PCM
        PCM16,      // 16-bit PCM
        PCM24,      // 24-bit PCM
        PCM32,      // 32-bit PCM (integer)
        FLOAT32,    // 32-bit float
        FLOAT64,    // 64-bit float (для Hi-Res)
        ALAW,       // 8-bit A-law
        ULAW,       // 8-bit μ-law
        IMA_ADPCM,  // IMA ADPCM
        MIC_ADPCM,  // MICROSOFT ADPCM
        GSM_6,      // GSM 6
        MP3,        // MPEG Layer III
        AAC,        // Advanced Audio Coding
        OPUS,       // Opus
        VORBIS,     // Ogg Vorbis
        ALAC,       // Apple Lossless
        WAVPACK,    // WavPack lossless
        FLAC,
        OTHER
    }

    public TrackFormat {
        validateField(channels);
        validateField(bitDepth);
        validateField(sampleRate);
    }

    private void validateField(int field) {
        if (field <= 0) {
            throw new IllegalArgumentException("Parameter must be greater than 0!");
        }
    }

    /**
     * Calculates bitrate for uncompressed PCM audio.
     * For compressed formats returns estimated or 0.
     */
    public int bitRate() {
        return sampleRate * bitDepth * channels;
    }

    /**
     * Calculate bytes per sample for uncompressed PCM audio.
     *
     * @return audio bytes per sample.
     */
    public int bytesPerSample() {
        return (bitDepth / 8) * channels;
    }

    @Contract(" -> new")
    public @NonNull AudioFormat toAudioFormat() {
        return toAudioFormat(this);
    }

    @Contract("_ -> new")
    public static @NonNull AudioFormat toAudioFormat(@NonNull TrackFormat format) {
        return new AudioFormat(format.channels, format.sampleRate);
    }

    @Override
    public String toString() {
        return String.format("%dHz, %dch, %dbit, %s, %s. AudioCodec: %s",
                sampleRate, channels, bitDepth,
                signed ? "signed" : "unsigned",
                byteOrder == ByteOrder.BIG_ENDIAN ? "BE" : "LE", audioCodec.name());
    }
}