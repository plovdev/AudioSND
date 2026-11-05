package org.plovdev.audioengine.api;

import org.jetbrains.annotations.NotNull;
import org.plovdev.audioengine.api.source.AudioProducer;
import org.plovdev.audioengine.api.source.AudioSource;
import org.plovdev.audioengine.api.source.Track;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.devices.NativeInputAudioDevice;
import org.plovdev.audioengine.effects.GainEffect;
import org.plovdev.audioengine.exceptions.devices.OpenAudioDeviceException;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.metadata.TrackMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Native implementation of AudioRecorder using platform-specific audio input.
 * <p>
 * Records audio from input device with configurable format and gain control.
 * Uses virtual threads for event handling and direct buffers for performance.
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see AudioRecorder
 * @see NativeInputAudioDevice
 */
public class NativeAudioRecorder implements AudioRecorder {
    private static final Logger log = LoggerFactory.getLogger(NativeAudioRecorder.class);
    private final ExecutorService eventExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final AtomicInteger chunkSize = new AtomicInteger(2048);
    private volatile float gain = 1f;

    private final AtomicBoolean isInited = new AtomicBoolean(false);
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicReference<AudioStatus> status = new AtomicReference<>(AudioStatus.UNAVAILABLE);
    private final GainEffect gainEffect;

    private final AudioProducer recorderProducer = new AudioProducer() {
        @Override
        public void setChunkSize(int size) {
            chunkSize.set(size);
        }

        @Override
        public int loadNextChunk(Deque<ByteBuffer> queueToLoad) {
            if (isInited.get()) {
                if (isRecording.get()) {
                    ByteBuffer chunk = ByteBuffer.allocateDirect(getChunkSize());
                    int bytesRead = audioDevice.read(chunk);

                    if (bytesRead > 0) {
                        processChunk(chunk);
                        queueToLoad.offer(chunk);
                        return bytesRead;
                    }
                }
            }

            return getChunkSize();
        }

        @Override
        public void awaitChunkLoaded() {
            // NO-OP
        }

        @Override
        public void setPointer(long position) {
            throw new UnsupportedOperationException("Unable set pointer");
        }

        @Override
        public long limit() {
            return -1; // infinity
        }
    };

    private final NativeInputAudioDevice audioDevice;
    private final AudioFormat audioFormat;

    private Runnable onStatusChanged = () -> {
    };
    private Runnable onChunkRecorded = () -> {
    };

    /**
     * Creates a new native audio recorder.
     *
     * @param format desired recording format (sample rate, channels, etc.)
     * @param info   input device to use
     * @throws NullPointerException     if format or info is null
     * @throws OpenAudioDeviceException if device cannot be opened
     */
    public NativeAudioRecorder(AudioFormat format, AudioDeviceInfo info) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(info);
        this.audioFormat = format;
        this.gainEffect = new GainEffect(gain);
        audioDevice = new NativeInputAudioDevice(info);

        init();
        log.debug("Native audio recorder initialized successfully");
    }

    /**
     * Initializes the recorder and opens audio device.
     * Called automatically in constructor.
     *
     * @throws OpenAudioDeviceException if device cannot be opened
     */
    @Override
    public synchronized void init() {
        if (!isInited.get()) {
            try {
                audioDevice.open(audioFormat);
                isInited.set(true);
                setStatus(AudioStatus.INITED);
            } catch (OpenAudioDeviceException e) {
                setStatus(AudioStatus.UNAVAILABLE);
                throw e;
            }
        }
    }

    /**
     * Starts recording from input device.
     * Launches dedicated recording thread.
     *
     * @throws IllegalStateException if not initialized
     */
    @Override
    public synchronized Track start() {
        checkIfInited();

        if (isRecording.get()) {
            throw new IllegalStateException("Audio already recording");
        }

        isRecording.set(true);
        setStatus(AudioStatus.RUNNING);
        return new Track(new AudioSource(recorderProducer), Duration.ofMillis(-1), audioFormat, TrackMetadata.DEFAULT_METADATA);
    }

    /**
     * Pauses recording at current position.
     * Recording can be resumed with {@link #start()}.
     *
     * @throws IllegalStateException if not initialized
     */
    @Override
    public synchronized void pause() {
        checkIfInited();
        isRecording.set(false);
        setStatus(AudioStatus.PAUSED);
    }


    @Override
    public synchronized void stop() {
        checkIfInited();

        isRecording.set(false);
        //TODO: clear source
        setStatus(AudioStatus.STOPPED);
    }

    /**
     * Sets input gain (amplification) for recording.
     *
     * @param gain multiplier (0.0 = silent, 1.0 = normal, >1.0 = boost)
     * @throws IllegalArgumentException if gain <= 0
     */
    @Override
    public synchronized void setGain(float gain) {
        this.gain = Math.clamp(gain, -10, 10);
        gainEffect.setGain(this.gain);
    }

    /**
     * Processes audio chunk through gain effect.
     *
     * @param chunk raw audio chunk
     */
    @NotNull
    private void processChunk(ByteBuffer chunk) {
        gainEffect.processInPlace(chunk);
    }

    /**
     * Returns current audio device being used.
     */
    @Override
    public AudioDeviceInfo getCurrentAudioDevice() {
        return audioDevice.getDeviceInfo();
    }

    /**
     * Returns current recorder status.
     */
    @Override
    public AudioStatus getStatus() {
        return status.get();
    }

    /**
     * Returns current recording format.
     */
    @Override
    public AudioFormat getCurrentFormat() {
        return audioFormat;
    }

    /**
     * Updates recorder status and triggers callback.
     *
     * @param newStatus new status
     */
    private synchronized void setStatus(AudioStatus newStatus) {
        if (this.status.get() != newStatus) {
            this.status.set(newStatus);
            try {
                eventExecutor.execute(onStatusChanged);
            } catch (Exception e) {
                log.error("Error in status change callback", e);
            }
        }
    }

    /**
     * Checks if recorder is initialized.
     *
     * @throws IllegalStateException if not initialized
     */
    private void checkIfInited() {
        if (!isInited.get()) {
            throw new IllegalStateException("AudioRecorder is not initialized");
        }
    }

    /**
     * Returns chunk size in bytes.
     */
    public int getChunkSize() {
        return chunkSize.get();
    }

    /**
     * Returns current gain value.
     */
    public float getGain() {
        return gain;
    }

    /**
     * Returns initialization state.
     */
    public boolean isInited() {
        return isInited.get();
    }

    /**
     * Returns current status change callback.
     */
    public Runnable getOnStatusChanged() {
        return onStatusChanged;
    }

    /**
     * Sets callback for status changes.
     *
     * @param onStatusChanged callback to run when status changes
     */
    public void setOnStatusChanged(Runnable onStatusChanged) {
        this.onStatusChanged = onStatusChanged;
    }

    /**
     * Returns current chunk recorded callback.
     */
    public Runnable getOnChunkRecorded() {
        return onChunkRecorded;
    }

    /**
     * Sets callback for each recorded chunk.
     * Useful for monitoring or real-time processing.
     *
     * @param onChunkRecorded callback to run after each chunk is recorded
     */
    public void setOnChunkRecorded(Runnable onChunkRecorded) {
        this.onChunkRecorded = onChunkRecorded;
    }

    /**
     * Releases all resources and stops recording.
     */
    @Override
    public void close() {
        if (isInited.get()) {
            isRecording.set(false);
            audioDevice.close();
            isInited.set(false);
            setStatus(AudioStatus.UNAVAILABLE);
            eventExecutor.close();
        }
    }
}