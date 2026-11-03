package org.plovdev.audioengine.api;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.devices.AudioDeviceStatus;
import org.plovdev.audioengine.devices.NativeOutputAudioDevice;
import org.plovdev.audioengine.effects.GainEffect;
import org.plovdev.audioengine.exceptions.devices.OpenAudioDeviceException;
import org.plovdev.audioengine.format.AudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Controls playback of a single audio track.
 * <p>
 * Player follows strict lifecycle:
 * <ol>
 *   <li>{@link #play()} - start playback</li>
 *   <li>{@link #pause()} - pause playback</li>
 *   <li>{@link #stop()} - stop playback</li>
 *   <li>{@link #close()} - release all resources</li>
 * </ol>
 * </p>
 * <p>
 * Additional controls:
 * <ul>
 *   <li>{@link #setVolume(float)} - adjust volume in real-time</li>
 *   <li>{@link #setSpeed(float)} - change playback speed</li>
 *   <li>{@link #setLoopCount(int)} - configure looping</li>
 *   <li>{@link #seek(Duration)} - seek to position</li>
 *   <li>{@link #setAudioDevice(AudioDeviceInfo)} - switch output device on the fly</li>
 * </ul>
 * </p>
 *
 * @author Anton
 * @version 1.0
 * @see Track
 * @see AudioRecorder
 */
public class NativeTrackPlayer implements TrackPlayer {
    private static final Logger log = LoggerFactory.getLogger(NativeTrackPlayer.class);
    private static final int BYTES_PER_SAMPLE = Float.BYTES;
    private final ExecutorService eventExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private float volume = 1f;
    private int totalCycles = 1;

    private final AtomicLong position = new AtomicLong(0L);
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isInited = new AtomicBoolean(false);
    private final AtomicReference<NativeOutputAudioDevice> audioDeviceReference;
    private final GainEffect gainEffect;

    private final AtomicReference<AudioStatus> status = new AtomicReference<>(AudioStatus.UNAVAILABLE);
    private final Track track;
    private final AudioFormat audioFormat;
    private final ByteBuffer data;

    private int chunkSize = 1024;
    private int currentCycle = 0;

    private Runnable onStatusChanged = () -> {
    };
    private Runnable onChunkPlayed = () -> {
    };


    /**
     * Creates a new native track player for the specified track and output device.
     * <p>
     * Initializes the player with:
     * <ul>
     *   <li>Track data and format</li>
     *   <li>Output audio device</li>
     *   <li>Internal buffers and effects</li>
     * </ul>
     * </p>
     *
     * @param track audio track to play (must be loaded and valid)
     * @param info  output device info (must be OUTPUT type)
     * @throws NullPointerException if track or info is null
     */
    public NativeTrackPlayer(Track track, AudioDeviceInfo info) {
        Objects.requireNonNull(track);
        Objects.requireNonNull(info);

        this.track = track;
        this.audioFormat = track.getFormat();
        this.gainEffect = new GainEffect(volume);
        audioDeviceReference = new AtomicReference<>(new NativeOutputAudioDevice(info));
        data = ByteBuffer.allocateDirect(track.getTrackData().capacity());
        data.put(track.getTrackData());

        init();
        log.debug("Native track player inited success");
    }


    /**
     * Initializes the player with track format and opens audio device.
     * Called automatically in constructor, but can be called manually if needed.
     *
     * @throws OpenAudioDeviceException if cann't init device.
     */
    @Override
    public synchronized void init() {
        if (!isInited.get()) {
            try {
                audioDeviceReference.get().open(track.getFormat());
                isInited.set(true);
                setStatus(AudioStatus.INITED);
            } catch (OpenAudioDeviceException e) {
                setStatus(AudioStatus.UNAVAILABLE);
                throw new OpenAudioDeviceException(e.getMessage());
            }
        }
    }

    /**
     * Starts or resumes playback from current position.
     * If player is stopped, starts from beginning.
     * If player is paused, resumes from paused position.
     *
     * @throws IllegalStateException if player is not initialized
     */
    @Override
    public synchronized void play() {
        checkIfInited();

        if (isPlaying.get()) {
            return;
        }

        isPlaying.set(true);
        setStatus(AudioStatus.RUNNING);

        Thread audioLoopThread = new Thread(this::audioLoop, "audio-loop");
        audioLoopThread.setPriority(Thread.MAX_PRIORITY);
        audioLoopThread.setDaemon(true);
        audioLoopThread.start();
    }

    /**
     * Seeks to specific position in track.
     * Position is clamped to track duration.
     *
     * @param position target position to seek to
     * @throws IllegalStateException if player is not initialized
     */
    @Override
    public void seek(@NonNull Duration position) {
        checkIfInited();

        long frames = (long) (position.toNanos() / 1_000_000_000.0 * audioFormat.sampleRate());
        long toPosition = frames * audioFormat.channels() * BYTES_PER_SAMPLE;

        long limit = data.limit();
        if (toPosition >= limit) {
            toPosition = limit;
        }
        this.position.set(toPosition);
    }

    /**
     * Pauses playback at current position.
     * Playback can be resumed with {@link #play()}.
     *
     * @throws IllegalStateException if player is not playing
     */
    @Override
    public synchronized void pause() {
        checkIfInited();
        isPlaying.set(false);
    }

    /**
     * Stops playback and resets position to beginning.
     * Player remains initialized and can be played again.
     *
     * @throws IllegalStateException if player is not active
     */
    @Override
    public synchronized void stop() {
        checkIfInited();

        isPlaying.set(false);
        position.set(0);
    }


    /**
     * Gets current playback volume.
     *
     * @return volume value (typically 0.0 = silent, 1.0 = normal, >1.0 = boost)
     */
    @Override
    public float getVolume() {
        return volume;
    }

    /**
     * Sets playback volume in real-time.
     *
     * @param volume new volume.
     */
    @Override
    public synchronized void setVolume(float volume) {
        this.volume = Math.clamp(volume, -10.0f, 10.0f);
        gainEffect.setGain(this.volume);
    }

    /**
     * Gets loop (repeat) count.
     *
     * @return number of times to repeat (0 = no loop, -1 = infinite)
     */
    @Override
    public int getLoopCount() {
        return totalCycles;
    }

    /**
     * Sets loop (repeat) count.
     *
     * @param count number of times to repeat (0 = no loop, -1 = infinite)
     */
    @Override
    public synchronized void setLoopCount(int count) {
        this.totalCycles = count;
        log.debug("Loop count set to: {}", count == -1 ? "infinite" : count);
    }

    /**
     * Gets current playing cycle number.
     *
     * @return current cycle index.
     */
    @Override
    public int getCurrentCycle() {
        return currentCycle;
    }

    /**
     * Returns current player status.
     *
     * @return current status (RUNNING, PAUSED, STOPPED, etc.)
     */
    @Override
    public AudioStatus getStatus() {
        return status.get();
    }

    /**
     * Returns current track format.
     *
     * @return current format.
     */
    @Override
    public AudioFormat getCurrentFormat() {
        return track.getFormat();
    }

    /**
     * Checks if player is initialized.
     *
     * @return true if player is ready
     */
    public boolean isInited() {
        return isInited.get();
    }

    /**
     * Gets current playback time position.
     *
     * @return elapsed time from start of track
     */
    @Override
    public synchronized Duration getCurrentTime() {
        long frames = position.get() / ((long) audioFormat.channels() * BYTES_PER_SAMPLE);
        double seconds = (double) frames * audioFormat.sampleRate();
        return Duration.ofNanos((long) (seconds * 1_000_000_000L));
    }

    /**
     * Gets current audio device info.
     *
     * @return device information of current output device
     */
    @Override
    public AudioDeviceInfo getCurrentAudioDevice() {
        return audioDeviceReference.get().getDeviceInfo();
    }

    /**
     * Switches audio output device during playback.
     * Player will pause briefly during device switch and resume automatically.
     *
     * @param newOutDevice new output device info
     * @throws NullPointerException if device is null
     */
    @SuppressWarnings("resource")
    @Override
    public synchronized void setAudioDevice(AudioDeviceInfo newOutDevice) {
        Objects.requireNonNull(newOutDevice, "Audio device cannot be null");
        log.debug("Re-init audio device. Old device: {}, new device: {}", audioDeviceReference.get(), newOutDevice);
        pause();

        setOnStatusChanged(() -> {
            if (status.get() == AudioStatus.STOPPED) {
                NativeOutputAudioDevice newOutput = new NativeOutputAudioDevice(newOutDevice);
                newOutput.setOnStatusChanged(() -> {
                    if (newOutput.getDeviceStatus() == AudioDeviceStatus.OPENED) {
                        audioDeviceReference.get().close();
                        audioDeviceReference.set(newOutput);
                        play();
                        newOutput.setOnStatusChanged(() -> {
                        }); // зануляем
                    }
                });
                newOutput.open(track.getFormat());
                setOnStatusChanged(() -> {
                });
            }
        });
    }

    /**
     * Main audio playback loop.
     * <p>
     * Runs in a separate high-priority thread and handles:
     * <ul>
     *   <li>Reading audio data from track</li>
     *   <li>Applying effects via {@link #processChunk(ByteBuffer)}</li>
     *   <li>Writing processed data to audio device</li>
     *   <li>Looping logic (finite/infinite cycles)</li>
     * </ul>
     * </p>
     * <p>
     * Thread stops when {@link #isPlaying} becomes false or track ends.
     * </p>
     */
    private void audioLoop() {
        checkIfInited();
        if (totalCycles == 0) return;
        long limit = data.limit();

        NativeOutputAudioDevice audioDevice = audioDeviceReference.get();
        while (isPlaying.get()) {
            int start = (int) position.get();
            int rem = (int) (limit - start);

            if (start >= limit || rem <= 0) {
                currentCycle++;
                if (totalCycles < 0) {
                    position.set(0);
                    continue;
                } else if (currentCycle < totalCycles) {
                    position.set(0);
                    continue;
                } else {
                    stop();
                    break;
                }
            }

            ByteBuffer chunk = data.slice(start, Math.min(chunkSize, rem));
            processChunk(chunk);
            audioDevice.write(chunk);

            position.set(Math.min(start + chunkSize, limit));
            eventExecutor.execute(onChunkPlayed);
        }

        log.debug("Stop playing");
        setStatus(AudioStatus.STOPPED);
    }

    //=========== UTILS ===========\\

    /**
     * Checks if player is initialized.
     *
     * @throws IllegalStateException if player not initialized
     */
    private void checkIfInited() {
        if (!isInited.get()) {
            throw new IllegalStateException("TrackPlayer is not ready!");
        }
    }

    /**
     * Registers callback for player status changes.
     *
     * @param onChange runnable to execute when status changes
     */
    public void setOnStatusChanged(Runnable onChange) {
        onStatusChanged = onChange;
    }

    /**
     * Gets current status change callback.
     *
     * @return current status listener
     */
    public Runnable getOnStatusChanged() {
        return onStatusChanged;
    }


    /**
     * Gets callback for chunk playback events.
     *
     * @return runnable that executes on each chunk played
     */
    public Runnable getOnChunkPlayed() {
        return onChunkPlayed;
    }

    /**
     * Sets callback to execute on each chunk playback.
     * Useful for visualizers or progress monitoring.
     *
     * @param onChunkPlayed runnable to execute per chunk
     */
    public void setOnChunkPlayed(Runnable onChunkPlayed) {
        this.onChunkPlayed = onChunkPlayed;
    }


    /**
     * Processes audio chunk through gain effect.
     * <p>
     * Currently applies only {@link GainEffect} for volume control.
     * In future versions may support full effect chain.
     * </p>
     *
     * @param chunk raw audio chunk to process
     */
    @NotNull
    private void processChunk(ByteBuffer chunk) {
        gainEffect.processInPlace(chunk);
    }

    /**
     * Updates player status and triggers callback if status changed.
     *
     * @param status new status
     */
    private synchronized void setStatus(AudioStatus status) {
        if (this.status.get() != status) { // Только при реальном изменении
            this.status.set(status);
            try {
                onStatusChanged.run();
            } catch (Exception e) {
                log.error("Error in status change callback", e);
            }
        }
    }

    public void setChunkSize(int newSize) {
        this.chunkSize = newSize;
    }

    /**
     * Closes player and releases all native resources.
     * Player cannot be used after closing.
     * <p>
     * Safe to call multiple times.
     * </p>
     */
    @Override
    public void close() {
        if (isInited.get()) {
            stop();
            audioDeviceReference.get().close();
            setStatus(AudioStatus.UNAVAILABLE);
            eventExecutor.close();
        }
    }
}