package org.plovdev.audioengine.devices;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер для получения аудио устройств.
 * Дает доступ к устройствам ввода и вывода звука на устройстве.
 *
 * @author Anton
 * @version 1.0
 * @see AudioDeviceInfo
 * @see AudioDevice
 */
public class AudioDeviceManager {
    private static final Logger log = LoggerFactory.getLogger(AudioDeviceManager.class);
    private volatile static AudioDeviceManager INSTANCE = null;
    private final List<AudioDeviceListener> deviceListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService callbackExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Singleton constructor
     */
    private AudioDeviceManager() {
        log.debug("Creating singleton instance for NativeAudioDeviceManager");
        _initManager();
    }

    /**
     * Метод для получения инстанса NativeAudioDeviceManager.h.
     *
     * @return NativeAudioDeviceManager singleton instance
     */
    public static AudioDeviceManager getInstance() {
        if (INSTANCE == null) {
            synchronized (AudioDeviceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AudioDeviceManager();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Ищет устройство ввода звука по его id.
     *
     * @param id id устройства ввода
     * @return устройство ввода
     */
    public AudioDeviceInfo getInputDeviceById(String id) {
        log.debug("Searching input audio device with id: {}", id);
        return getAudioDevice(id, getInputAudioDevices());
    }

    /**
     * Ищет устройство вывода звука по его id.
     *
     * @param id id устройства вывода
     * @return устройство вывода
     */
    public AudioDeviceInfo getOutputDeviceById(String id) {
        log.debug("Searching output audio device with id: {}", id);
        return getAudioDevice(id, getOutputAudioDevices());
    }

    @Contract("null, _ -> fail; !null, null -> fail")
    private @NonNull AudioDeviceInfo getAudioDevice(String id, List<AudioDeviceInfo> infos) {
        if (id == null) throw new IllegalArgumentException("ID cannot be null");
        if (infos == null) throw new IllegalArgumentException("Devices list cannot be null");

        return infos.stream().filter(info -> info.id().equals(id)).findFirst().orElseThrow(() -> new NoSuchElementException(String.format("Audio device with ID '%s' not found", id)));
    }

    /**
     * Возвращает список доступных устройств ввода.
     *
     * @return доступные устройства ввода
     */
    public List<AudioDeviceInfo> getInputAudioDevices() {
        return List.copyOf(_getInputAudioDevices());
    }

    /**
     * Возвращает список доступных устройств вывода.
     *
     * @return доступные устройства вывода
     */
    public List<AudioDeviceInfo> getOutputAudioDevices() {
        return List.copyOf(_getOutputAudioDevices());
    }

    /**
     * Возвращает стандартное системное устройство ввода.
     *
     * @return стандартное устройство ввода
     */
    public AudioDeviceInfo getDefaultInputAudioDevice() {
        return _getDefaultInputAudioDevice();
    }

    /**
     * Возвращает стандартное системное устройство вывода.
     *
     * @return стандартное устройство вывода
     */
    public AudioDeviceInfo getDefaultOutputAudioDevice() {
        return _getDefaultOutputAudioDevice();
    }


    //==========LISTENERS==========\\

    public void addAudioDeviceListener(AudioDeviceListener listener) {
        if (listener != null) {
            deviceListeners.add(listener);
        }
    }

    public void removeDeviceListener(AudioDeviceListener listener) {
        if (listener != null) {
            deviceListeners.remove(listener);
        }
    }

    public List<AudioDeviceListener> getDeviceListeners() {
        return List.copyOf(deviceListeners);
    }

    private void notifyConnected(AudioDeviceInfo info) {
        for (AudioDeviceListener listener : deviceListeners) {
            callbackExecutor.execute(() -> listener.onDeviceConnected(info));
        }
    }

    private void notifyDisconnected(AudioDeviceInfo info) {
        for (AudioDeviceListener listener : deviceListeners) {
            callbackExecutor.execute(() -> listener.onDeviceDisconnected(info));
        }
    }


    //==========NATIVIES==========\\

    private native void _initManager();

    private native AudioDeviceInfo _getDefaultInputAudioDevice();

    private native AudioDeviceInfo _getDefaultOutputAudioDevice();

    private native List<AudioDeviceInfo> _getInputAudioDevices();

    private native List<AudioDeviceInfo> _getOutputAudioDevices();
}