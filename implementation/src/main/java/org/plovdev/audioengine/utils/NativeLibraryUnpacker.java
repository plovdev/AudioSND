package org.plovdev.audioengine.utils;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import static org.plovdev.audioengine.engineconfig.AudioEngineConfig.NativeLib;

public class NativeLibraryUnpacker {
    private static final Logger log = LoggerFactory.getLogger(NativeLibraryUnpacker.class);
    private NativeLib toUnpack;

    public NativeLibraryUnpacker(NativeLib nativeLib) {
        toUnpack = nativeLib;
    }

    public NativeLib getToUnpack() {
        return toUnpack;
    }

    public void setToUnpack(NativeLib toUnpack) {
        this.toUnpack = toUnpack;
    }

    /**
     * Распаковывает нативную библиотеку во временный файл
     * Thread-safe и безопасный
     */
    public synchronized String unpackLib() throws Exception {
        String libName = getLibName();
        log.debug("Unpacking native lib: {}", libName);

        Path tempLibPath = createTempLib(libName);
        configurateLib(tempLibPath);
        extractLib(tempLibPath, libName);

        return tempLibPath.toString();
    }

    private Path createTempLib(String name) throws IOException {
        String unpackPath = System.getProperty("java.io.tmpdir") + File.separator;
        return Files.createTempFile(Path.of(unpackPath), "audiosnd_", name);
    }

    private void configurateLib(@NonNull Path path) {
        File file = path.toFile();
        file.deleteOnExit();
        log.debug("Can executable: {}", file.setExecutable(true, true));
    }

    private void extractLib(Path tempLibPath, String libName) {
        try (InputStream libStream = Objects.requireNonNull(getClass().getResourceAsStream("/natives/libs/" + libName))) {
            Files.copy(libStream, tempLibPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Native lib unpacked to {}", tempLibPath);
        } catch (Exception e) {
            throw new RuntimeException("Cann't unpack native library: ", e);
        }
    }

    private @NonNull String getLibName() {
        String osName = System.getProperty("os.name").trim().toLowerCase();
        log.debug("OS name: {}", osName);
        if (osName.contains("win")) {
            return toUnpack + ".dll";
        } else if (osName.contains("mac")) {
            return "lib" + toUnpack + ".dylib";
        } else {
            return toUnpack + ".so";
        }
    }
}