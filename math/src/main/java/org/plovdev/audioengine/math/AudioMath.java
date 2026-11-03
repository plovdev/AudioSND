package org.plovdev.audioengine.math;

/**
 * Utility math functions for audio processing.
 *
 * @author Anton
 * @version 1.0
 */
public class AudioMath {

    private AudioMath() {
        throw new UnsupportedOperationException("Utility class - do not instantiate!");
    }

    /**
     * Clamps value to [min, max] range.
     */
    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Linear interpolation: start + (end - start) * t
     */
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    /**
     * Converts decibels to linear amplitude.
     *
     * @param db decibels
     * @return linear amplitude
     */
    public static float dbToLinear(float db) {
        return (float) Math.pow(10.0, db / 20.0);
    }

    /**
     * Converts linear amplitude to decibels.
     *
     * @param linear amplitude
     * @return decibels
     */
    public static float linearToDb(float linear) {
        return (float) (20.0 * Math.log10(linear));
    }

    /**
     * Converts semitones to frequency ratio.
     *
     * @param semitones number of semitones
     * @return frequency multiplier
     */
    public static float semitonesToRatio(int semitones) {
        return (float) Math.pow(2.0, semitones / 12.0);
    }

    /**
     * Converts frequency ratio to semitones.
     *
     * @param ratio frequency multiplier
     * @return number of semitones
     */
    public static float ratioToSemitones(float ratio) {
        return (float) (12.0 * Math.log(ratio) / Math.log(2.0));
    }

    /**
     * MIDI note number to frequency (A4 = 440Hz, MIDI note 69).
     */
    public static float midiToFreq(int midiNote) {
        return 440.0f * (float) Math.pow(2.0, (midiNote - 69) / 12.0);
    }

    /**
     * Frequency to nearest MIDI note number.
     */
    public static int freqToMidi(float freq) {
        return (int) Math.round(69 + 12 * Math.log(freq / 440.0) / Math.log(2));
    }

    /**
     * Maps value from one range to another.
     */
    public static float map(float value, float fromMin, float fromMax, float toMin, float toMax) {
        return toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin);
    }

    /**
     * Normalizes value to [0, 1] range.
     */
    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    /**
     * Fast approximation of sine (for LFOs, etc.)
     */
    public static float fastSin(float x) {
        // Parabolic approximation
        float y = (float) (1.27323954 * x - 0.405284735 * x * x);
        return y < 0 ? 0.225f * (y * -y - y) + y : 0.225f * (y * y - y) + y;
    }

    /**
     * Checks if value is power of two.
     */
    public static boolean isPowerOfTwo(int x) {
        return x > 0 && (x & (x - 1)) == 0;
    }

    /**
     * Next power of two.
     */
    public static int nextPowerOfTwo(int x) {
        if (x < 0) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    /**
     * Mixes two samples with crossfade.
     *
     * @param a first sample
     * @param b second sample
     * @param t mix factor (0 = only 'a', 1 = only 'b')
     */
    public static float mix(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    /**
     * Hard limiter.
     */
    public static float limit(float value, float limit) {
        return Math.max(-limit, Math.min(value, limit));
    }
}