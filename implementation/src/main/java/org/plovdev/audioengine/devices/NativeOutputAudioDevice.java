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
 * Класс, который обеспечивает запись в аудио устройства.
 *
 * @author Anton
 * @version 1.0
 */
public final class NativeOutputAudioDevice implements OutputAudioDevice {
    private long nativeHandle = 0;

    private static final Logger log = LoggerFactory.getLogger(NativeOutputAudioDevice.class);
    private final AudioDeviceInfo info;
    private AudioFormat trackFormat;
    private volatile AudioDeviceStatus status = UNAVAILABLE;
    private final AtomicBoolean isInited = new AtomicBoolean(false);
    private Runnable onStatusChanged = () -> {
    };

    /**
     * Создает экземпляр для работы над аудио устройстовм
     *
     * @param info устройство с которым будет работать класс.
     */
    public NativeOutputAudioDevice(@NotNull AudioDeviceInfo info) {
        Objects.requireNonNull(info);
        if (info.type() == AudioDeviceInfo.AudioDeviceType.INPUT) {
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
            log.warn("Audio device {} already opened with format: {}", info.id(), trackFormat);
            return;
        }

        if (!isSupportedFormat(format)) {
            log.warn("Device {} does not support format: {}", info.id(), format);
        }

        try {
            setStatus(OPENING);
            trackFormat = format;
            nativeHandle = _open(format, info);
            isInited.set(true);
            setStatus(OPENED);
            log.debug("Audio output device {} opened", info.id());
        } catch (Throwable e) {
            log.error("Initiliazing error: ", e);
            setStatus(ERROR);
            trackFormat = null;
            throw new OpenAudioDeviceException("Failed to open device");
        }
    }

    /**
     * Write data to audio device
     *
     * @param byteBuffer data to write
     * @return wrote bytes
     */
    @Override
    public synchronized int write(@NotNull ByteBuffer byteBuffer) {
        checkDevice();
        try {
            status = RUNNING;
            return _write(byteBuffer, nativeHandle);
        } finally {
            status = OPENED;
        }
    }

    private void checkDevice() {
        checkForInited();

        if (status == ERROR || status == CLOSING || status == RUNNING) {
            throw new AudioDeviceException(String.format("Device %s is in %s state", info.id(), status));
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
        if (isInited.get()) {
            try {
                setStatus(CLOSING);
                _close(nativeHandle);
                isInited.set(false);
                setStatus(CLOSED);
                log.debug("Audio device {} closed", info.id());
            } catch (Exception e) {
                log.error("Closing error: ", e);
                setStatus(ERROR);
                throw new CloseAudioDeviceException("Failed to close device");
            } finally {
                trackFormat = null;
            }
        } else {
            log.warn("Audio device {} not initialized for closing.", info.id());
        }
    }

    /**
     * Set listener, which called when audio device status changing
     *
     * @param onChange new listener
     */
    public synchronized void setOnStatusChanged(Runnable onChange) {
        Objects.requireNonNull(onChange);
        onStatusChanged = onChange;
    }

    private synchronized void setStatus(AudioDeviceStatus status) {
        if (this.status != status) {
            this.status = status;
            try {
                onStatusChanged.run();
            } catch (Exception e) {
                log.error("Error in status change callback", e);
            }
        }
    }

    private void checkForInited() {
        if (!isInited.get()) {
            throw new AudioDeviceException(String.format("Audio device %s is not opened!", info.id()));
        }
    }

    /**
     * Return format, which audio device opened.
     *
     * @return format
     */
    public AudioFormat getTrackFormat() {
        return trackFormat;
    }

    public boolean isInited() {
        return isInited.get();
    }

    /**
     * @return status listener
     */
    public Runnable getOnStatusChanged() {
        return onStatusChanged;
    }

    @Override
    public String toString() {
        return info.toString();
    }

    //============NATIVIES============\\

    private native long _open(AudioFormat format, AudioDeviceInfo info);

    private native int _write(ByteBuffer buffer, long handle);

    private native int _write(long address, long start, long length, long handle);

    private native void _close(long handle);
}