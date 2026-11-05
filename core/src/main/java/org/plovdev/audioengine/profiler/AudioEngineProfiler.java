package org.plovdev.audioengine.profiler;

public interface AudioEngineProfiler {
    EngineSnapshot snapshot();

    long executionTime(Runnable r);
}