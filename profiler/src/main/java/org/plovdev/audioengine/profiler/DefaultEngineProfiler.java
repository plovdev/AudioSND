package org.plovdev.audioengine.profiler;

import com.sun.management.OperatingSystemMXBean;
import org.jspecify.annotations.NonNull;
import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.devices.AudioDeviceInfo;
import org.plovdev.audioengine.profiler.benchmarking.ProfileProcessor;
import org.plovdev.audioengine.profiler.benchmarking.ProfileResult;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultEngineProfiler implements AudioEngineProfiler {
    private final AudioEngine engine;
    public DefaultEngineProfiler(AudioEngine prof) {
        engine = prof;
    }

    @Override
    public EngineSnapshot snapshot() {
        Objects.requireNonNull(engine);

        OperatingSystemMXBean mxBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuUsage = mxBean.getProcessCpuLoad();

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.maxMemory() - runtime.freeMemory();

        long nativeMemoryUsed = mxBean.getCommittedVirtualMemorySize();
        int threads = Thread.activeCount();

        List<AudioDeviceInfo> infos = new ArrayList<>();
        infos.addAll(engine.getAvailableInputAudioDevices());
        infos.addAll(engine.getAvailableOutputAudioDevices());

        return new EngineSnapshot(System.currentTimeMillis(), cpuUsage, usedMemory, nativeMemoryUsed, threads, LoadedTrakCounter.getTracksCount(), infos);
    }

    public List<ProfileResult<?>> execProfiling(Object cls) {
        return ProfileProcessor.execProfile(cls);
    }

    @Override
    public long executionTime(@NonNull Runnable r) {
        long startExec = System.nanoTime();
        r.run();
        return System.nanoTime() - startExec; // nano sec
    }
}