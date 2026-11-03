package org.plovdev.audioengine.devices;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.exceptions.devices.AudioDeviceException;
import org.plovdev.audioengine.exceptions.devices.CloseAudioDeviceException;
import org.plovdev.audioengine.exceptions.devices.OpenAudioDeviceException;
import org.plovdev.audioengine.format.AudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.plovdev.audioengine.devices.AudioDeviceStatus.*;

/**
 * Класс, который обеспечивает чтение с аудио устройства.
 *
 * @author Anton
 * @version 1.0
 */
public final class NativeInputAudioDevice implements InputAudioDevice {
    private static final Logger log = LoggerFactory.getLogger(NativeInputAudioDevice.class);
    private final AudioDeviceInfo info;
    private volatile AudioDeviceStatus status = AudioDeviceStatus.UNAVAILABLE;
    private final AtomicBoolean isInited = new AtomicBoolean(false);
    private long nativeHandle = 0;

    private Runnable onStatusChanged = () -> {
    };

    /**
     * Создает экземпляр для работы над аудио устройстовм
     *
     * @param info устройство с которым будет работать класс.
     */
    public NativeInputAudioDevice(AudioDeviceInfo info) {
        Objects.requireNonNull(info);
        if (info.type() == AudioDeviceInfo.AudioDeviceType.OUTPUT) {
            throw new AudioDeviceException("Unsupported device type");
        }

        this.info = info;
    }

    /**
     * Open audio device.
     *
     * @param format working format
     * @throws OpenAudioDeviceException when opening failed.
     */
    @Override
    public synchronized void open(AudioFormat format) throws OpenAudioDeviceException {
        if (isInited.get()) {
            log.warn("Audio device {} already initialized.", info.id());
            return;
        }

        if (!isSupportedFormat(format)) {
            log.warn("Device {} does not support this format: {}. See supported formats in getDeviceInfo().supportedFormats()", info.id(), format);
            // если юзер хочет/знает что формат будет работать, то пусть пробует.
        }

        setStatus(OPENING);
        try {
            nativeHandle = _open(format, info);
            isInited.set(true);
            setStatus(OPENED);
            log.debug("Audio device {} opened", info.id());
        } catch (Throwable e) {
            setStatus(ERROR);
            throw new OpenAudioDeviceException("Fail to open audio device: ", e);
        }
    }

    /**
     * Read data from input audio device to buffer.
     *
     * @param byteBuffer buffer to read.
     * @return readed bytes.
     */
    @Override
    public int read(@NotNull ByteBuffer byteBuffer) {
        checkForInited();
        if (status == ERROR || status == CLOSING) {
            throw new AudioDeviceException(String.format("Device %s is in %s state", info.id(), status));
        }

        try {
            status = RUNNING;
            return _read(byteBuffer, nativeHandle);
        } finally {
            status = OPENED;
        }
    }


    /**
     * Return all info about audio device
     *
     * @return deivce info
     */
    @Override
    public AudioDeviceInfo getDeviceInfo() {
        return info;
    }

    /**
     * Return status of audio device, which as opened, closed, etc.
     *
     * @return device status
     */
    @Override
    public AudioDeviceStatus getDeviceStatus() {
        return status;
    }

    /**
     * Checks if the audio device is open.
     *
     * @return is audio device open.
     */
    @Override
    public boolean isOpen() {
        return isInited.get();
    }

    @Override
    public synchronized void close() throws CloseAudioDeviceException {
        if (!isInited.get()) {
            log.warn("Audio device {} not initialized for closing.", info.id());
            return;
        }

        setStatus(CLOSING);
        try {
            _close(nativeHandle);
            isInited.set(false);
            setStatus(CLOSED);
            log.debug("Audio device {} closed", info.id());
        } catch (Throwable e) {
            setStatus(ERROR);
            throw new CloseAudioDeviceException("Failed to close audio device: " + e.getMessage());
        }
    }

    public synchronized void setOnStatusChanged(Runnable onChange) {
        onStatusChanged = onChange;
    }

    private synchronized void setStatus(AudioDeviceStatus status) {
        if (this.status != status) {
            this.status = status;
            onStatusChanged.run();
        }
    }

    private void checkForInited() {
        if (!isInited.get()) {
            throw new AudioDeviceException("Audio device not opened!");
        }
    }

    public Runnable getOnStatusChanged() {
        return onStatusChanged;
    }

    @Override
    public String toString() {
        return info.toString();
    }

    private native long _open(AudioFormat format, AudioDeviceInfo info);

    private native int _read(ByteBuffer buffer, long handle);

    private native void _close(long handle);
}