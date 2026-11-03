package org.plovdev.audioengine.profiler.benchmarking;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ProfileProcessor {
    private static final Logger log = LoggerFactory.getLogger(ProfileProcessor.class);

    public static @NonNull List<ProfileResult<?>> execProfile(@NonNull Object profileClass) {
        List<ProfileResult<?>> results = new ArrayList<>();
        Method[] methods = profileClass.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Profile.class)) {
                try {
                    method.setAccessible(true);
                    long start = System.nanoTime();
                    Object o = method.invoke(profileClass);
                    long end = System.nanoTime();
                    results.add(new ProfileResult<>((end - start), o));
                } catch (Exception e) {
                    log.error("Error to exec profiling: ", e);
                }
            }
        }
        return results;
    }
}