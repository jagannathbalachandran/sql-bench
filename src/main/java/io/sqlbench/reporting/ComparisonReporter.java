package io.sqlbench.reporting;

import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.model.QueryResult;

import java.util.*;

public class ComparisonReporter {

    public record EngineComparison(
            String baseEngine, String compareEngine,
            int baseWins, int compareWins, int ties,
            double baseAvgMs, double compareAvgMs,
            double baseSpeedupPercent  // positive = base is faster
    ) {}

    public static List<EngineComparison> compare(String baseEngineName, List<BenchmarkSuiteResult> results) {
        BenchmarkSuiteResult base = results.stream()
                .filter(r -> r.getEngineName().equalsIgnoreCase(baseEngineName))
                .findFirst().orElse(null);
        if (base == null) return List.of();

        List<EngineComparison> comparisons = new ArrayList<>();
        Map<String, Long> baseTimes = extractTimes(base.getBestRun());

        for (BenchmarkSuiteResult other : results) {
            if (other.getEngineName().equalsIgnoreCase(baseEngineName)) continue;
            Map<String, Long> otherTimes = extractTimes(other.getBestRun());

            int baseWins = 0, otherWins = 0, ties = 0;
            for (Map.Entry<String, Long> e : baseTimes.entrySet()) {
                Long ot = otherTimes.get(e.getKey());
                if (ot == null) continue;
                int cmp = Long.compare(e.getValue(), ot);
                if (cmp < 0) baseWins++;
                else if (cmp > 0) otherWins++;
                else ties++;
            }

            double baseAvg = baseTimes.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double otherAvg = otherTimes.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double speedup = otherAvg > 0 ? ((otherAvg - baseAvg) / otherAvg) * 100 : 0;

            comparisons.add(new EngineComparison(
                    baseEngineName, other.getEngineName(),
                    baseWins, otherWins, ties,
                    baseAvg, otherAvg, speedup));
        }
        return comparisons;
    }

    private static Map<String, Long> extractTimes(BenchmarkRun run) {
        Map<String, Long> times = new LinkedHashMap<>();
        for (QueryResult r : run.getResults()) {
            if (r.isSuccess()) times.put(r.getQueryAlias(), r.getTotalClientTimeMs());
        }
        return times;
    }
}
