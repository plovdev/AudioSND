package org.plovdev.audioengine.loaders;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.exceptions.loaders.TrackLoadException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.List;

public class ExportUtils {
    public static byte @NonNull [] intToLittleEndian(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
    }

    public static byte @NonNull [] shortToLittleEndian(short value) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array();
    }

    public static byte @NonNull [] floatToLittleEndian(float value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
    }

    public static int bytesToInt(byte[] bytes, int offset, int length) {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value |= (bytes[offset + i] & 0xFF) << (8 * i); // little-endian
        }
        return value;
    }

    public static byte @NonNull [] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        }
    }

    public static byte @NonNull [] readStreamToByteArray(@NonNull InputStream stream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }
        return bos.toByteArray();
    }

    public static @NonNull File getFile(@NonNull File file, List<PathLocator> locators) {
        if (!file.exists()) {
            for (PathLocator locator : locators) {
                String newPath = locator.getPath().toString() + "/" + file.toPath();
                File newFile = new File(newPath);
                if (newFile.exists()) {
                    return newFile;
                }
            }
            throw new TrackLoadException("Track not found!");
        }
        return file;
    }

    public static Duration calculateDuration(long totalSamples, int sampleRate) {
        long seconds = totalSamples / sampleRate;
        long nanos = (totalSamples % sampleRate) * 1_000_000_000L / sampleRate;
        return Duration.ofSeconds(seconds).plusNanos(nanos);
    }
}