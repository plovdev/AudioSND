package org.plovdev.audioengine.generator;

import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.api.source.AudioProducer;
import org.plovdev.audioengine.format.AudioFormat;
import org.plovdev.audioengine.generator.config.GenerationConfig;
import org.plovdev.audioengine.generator.strategies.envelope.EnvelopeStrategy;
import org.plovdev.audioengine.generator.strategies.frequency.FrequencyStrategy;
import org.plovdev.audioengine.generator.strategies.modulation.ModulationStrategy;
import org.plovdev.audioengine.generator.strategies.noise.NoiseStrategy;
import org.plovdev.audioengine.generator.strategies.wave.WaveStrategy;
import org.plovdev.audioengine.math.AudioMath;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TrackGeneratorProducer implements AudioProducer, AutoCloseable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "generator-audio-producer");
        t.setDaemon(true);
        return t;
    });
    private final Semaphore loadRequests = new Semaphore(0, true);
    private final Semaphore chunksReady = new Semaphore(0, true);
    private final AtomicInteger chunkSize = new AtomicInteger(4096);

    private final GenerationConfig config;
    private final int sampleRate;
    private final int channels;
    private final long totalSamples;
    private final long totalSize;
    private final FrequencyStrategy freqStrategy;
    private final WaveStrategy waveStrategy;
    private final EnvelopeStrategy envStrategy;
    private final ModulationStrategy modulationStrategy;
    private final NoiseStrategy noiseStrategy;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean eof;

    public TrackGeneratorProducer(@NonNull Duration duration, @NonNull AudioFormat trackFormat, @NonNull GenerationConfig config) {
        this.config = config;
        this.sampleRate = trackFormat.sampleRate();
        this.channels = trackFormat.channels();
        this.totalSamples = duration.toMillis() * sampleRate / 1000;
        this.totalSize = totalSamples * channels * 4;

        this.freqStrategy = config.getFrequencyStrategy();
        this.waveStrategy = config.getWaveStrategy();
        this.envStrategy = config.getEnvelopeStrategy();
        this.modulationStrategy = config.getModulationStrategy();
        this.noiseStrategy = config.getNoiseStrategy();
    }

    @Override
    public void setChunkSize(int size) {
        chunkSize.set(size);
    }

    @Override
    public int loadNextChunk(Deque<ByteBuffer> queue) {
        if (started.compareAndSet(false, true)) {
            executor.submit(() -> pump(queue));
        }
        if (eof) return 0;
        loadRequests.release();
        return 1;
    }

    private void pump(Deque<ByteBuffer> queue) {
        try {
            while (!eof) {
                loadRequests.acquire();
                if (eof) break;

                ByteBuffer buf = loadNext();
                queue.add(buf);
                chunksReady.release();
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            eof = true;
            throw new RuntimeException(t);
        } finally {
            chunksReady.release();
        }
    }

    private @NonNull ByteBuffer loadNext() {
        int size = chunkSize.get();
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);

        for (long samplePos = 0; samplePos < size; samplePos++) {
            float[] channelSamples = new float[channels];

            for (int channel = 0; channel < channels; channel++) {
                float frequency = freqStrategy.getFrequency(channel, samplePos, totalSamples);

                if (modulationStrategy != null) {
                    frequency = modulationStrategy.modulate(samplePos, totalSamples, channel, frequency);
                }
                float sample = waveStrategy.generate(samplePos, frequency, config.getPhase(), sampleRate);
                float amplitude = envStrategy.getAmplitude(samplePos, totalSamples, 1);
                sample *= amplitude;

                if (config.getNoiseLevel() > 0 && noiseStrategy != null) {
                    float noise = noiseStrategy.nextSample(channel);
                    sample = sample * (1 - config.getNoiseLevel()) + noise * config.getNoiseLevel();
                }

                channelSamples[channel] = sample;
            }

            if (channels == 2) {
                float pan = config.getPan();
                float leftGain, rightGain;

                if (pan <= 0) {
                    leftGain = 1.0f;
                    rightGain = 1.0f + pan;
                } else {
                    leftGain = 1.0f - pan;
                    rightGain = 1.0f;
                }

                channelSamples[0] *= leftGain;
                channelSamples[1] *= rightGain;
            }

            for (int channel = 0; channel < channels; channel++) {
                float sample = AudioMath.clamp(channelSamples[channel], -1.0f, 1.0f);
                buffer.putFloat(sample);
            }
        }

        return buffer.flip();
    }

    @Override
    public void awaitChunkLoaded() throws InterruptedException {
        chunksReady.acquire();
    }

    @Override
    public void setPointer(long position) {

    }

    @Override
    public long limit() {
        return totalSize;
    }

    @Override
    public void close() {
        eof = true;
        loadRequests.release();
        executor.shutdownNow();
    }
}