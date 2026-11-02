package org.plovdev.audioengine.format;

/**
 * Always Float 32
 *
 * @param channels
 * @param sampleRate
 */
public record AudioFormat(int channels, int sampleRate) {
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