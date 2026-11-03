package org.plovdev.audioengine.profiler;

import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public class LoadedTrakCounter {
    private static final Logger log = LoggerFactory.getLogger(LoadedTrakCounter.class);

    public static int getTracksCount() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.sun.management:type=DiagnosticCommand");

            String result = (String) server.invoke(name, "gcClassHistogram", new Object[]{null}, new String[]{"[Ljava.lang.String;"});
            log.trace("Tracks count result: {}", result);

            return Arrays.stream(result.split("\n")).filter(LoadedTrakCounter::filterLine)
                    .mapToInt(line -> {
                        String[] parts = line.trim().split("\\s+");
                        return Integer.parseInt(parts[1]);
                    })
                    .findFirst()
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }
    @Contract(pure = true)
    private static boolean filterLine(@NonNull String line) {
        return line.contains("org.plovdev.audioengine.api.Track");
    }
}