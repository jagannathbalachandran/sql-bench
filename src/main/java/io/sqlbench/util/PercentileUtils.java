package io.sqlbench.util;

public class PercentileUtils {

    public static long percentile(long[] sorted, double p) {
        if (sorted == null || sorted.length == 0) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }
}
