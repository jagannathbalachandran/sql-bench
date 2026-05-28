package io.sqlbench.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    public static String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format("%.2fs", ms / 1000.0);
        long minutes = ms / 60_000;
        long seconds = (ms % 60_000) / 1000;
        return minutes + "m " + seconds + "s";
    }

    public static String nowIso() {
        return ISO.format(Instant.now());
    }
}
