package org.plovdev.audioengine.format;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import static org.plovdev.audioengine.format.AudioFormat.*;

public final class AudioFormatFactory {
    private AudioFormatFactory() {
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono8000() {
        return new AudioFormat(MONO, HZ8000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono16000() {
        return new AudioFormat(MONO, HZ16000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono24000() {
        return new AudioFormat(MONO, HZ24000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono32000() {
        return new AudioFormat(MONO, HZ32000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono44100() {
        return new AudioFormat(MONO, HZ44100);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono48000() {
        return new AudioFormat(MONO, HZ48000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono96000() {
        return new AudioFormat(MONO, HZ96000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat mono192000() {
        return new AudioFormat(MONO, HZ192000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo8000() {
        return new AudioFormat(STEREO, HZ8000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo16000() {
        return new AudioFormat(STEREO, HZ16000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo24000() {
        return new AudioFormat(STEREO, HZ24000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo32000() {
        return new AudioFormat(STEREO, HZ32000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo44100() {
        return new AudioFormat(STEREO, HZ44100);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo48000() {
        return new AudioFormat(STEREO, HZ48000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo96000() {
        return new AudioFormat(STEREO, HZ96000);
    }

    @Contract(" -> new")
    public static @NonNull AudioFormat stereo192000() {
        return new AudioFormat(STEREO, HZ192000);
    }
}