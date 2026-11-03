package org.plovdev.audioengine.format;

/**
 * Always Float 32
 *
 * @param channels
 * @param sampleRate
 */
public record AudioFormat(int channels, int sampleRate) {
    public static final int MONO = 1;
    public static final int STEREO = 2;

    public static final int HZ8000 = 8000;
    public static final int HZ16000 = 16000;
    public static final int HZ24000 = 24000;
    public static final int HZ32000 = 32000;
    public static final int HZ44100 = 44100;
    public static final int HZ48000 = 48000;
    public static final int HZ96000 = 96000;
    public static final int HZ192000 = 192000;

    public AudioFormat {
        validateField(channels);
        validateField(sampleRate);
    }

    private void validateField(int field) {
        if (field <= 0) {
            throw new IllegalArgumentException("Parameter must be greater than 0!");
        }
    }
}