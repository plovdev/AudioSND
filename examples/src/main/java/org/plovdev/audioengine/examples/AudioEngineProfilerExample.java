package org.plovdev.audioengine.examples;

import org.plovdev.audioengine.AudioEngine;
import org.plovdev.audioengine.NativeAudioEngine;
import org.plovdev.audioengine.profiler.DefaultEngineProfiler;
import org.plovdev.audioengine.profiler.benchmarking.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioEngineProfilerExample {
    private static final Logger log = LoggerFactory.getLogger(AudioEngineProfilerExample.class);

    void main() {
        try (AudioEngine engine = new NativeAudioEngine()) {
            DefaultEngineProfiler profiler = new DefaultEngineProfiler(engine);
            System.out.println(profiler.execProfiling(new AudioEngineProfilerExample()));
        } catch (Exception e) {
            log.error("Audio engine error: ", e);
        }
    }

    @Profile
    public String test() throws Exception {
        System.out.println("Hi from test");
        Thread.sleep(1000);
        return "Ok";
    }
}