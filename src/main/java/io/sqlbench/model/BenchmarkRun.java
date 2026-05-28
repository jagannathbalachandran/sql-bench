package io.sqlbench.model;

import io.sqlbench.util.PercentileUtils;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkRun {
    private final int runNumber;
    private final long startTimeMs;
    private long endTimeMs;
    private final List<QueryResult> results = new ArrayList<>();

    public BenchmarkRun(int runNumber) {
        this.runNumber = runNumber;
        this.startTimeMs = System.currentTimeMillis();
    }

    public void addResult(QueryResult result) {
        results.add(result);
    }

    public void finish() {
        this.endTimeMs = System.currentTimeMillis();
    }

    public BenchmarkMetrics computeMetrics() {
        List<QueryResult> successes = results.stream().filter(QueryResult::isSuccess).toList();
        long[] times = successes.stream()
                .mapToLong(QueryResult::getTotalClientTimeMs)
                .sorted()
                .toArray();

        long total = successes.stream().mapToLong(QueryResult::getTotalClientTimeMs).sum();

        return new BenchmarkMetrics(
                PercentileUtils.percentile(times, 50),
                PercentileUtils.percentile(times, 75),
                PercentileUtils.percentile(times, 90),
                PercentileUtils.percentile(times, 95),
                PercentileUtils.percentile(times, 99),
                total,
                results.size(),
                successes.size(),
                results.size() - successes.size()
        );
    }

    public int getRunNumber() { return runNumber; }
    public long getStartTimeMs() { return startTimeMs; }
    public long getEndTimeMs() { return endTimeMs; }
    public long getDurationMs() { return endTimeMs - startTimeMs; }
    public List<QueryResult> getResults() { return results; }
    public long getTotalSuccessTime() {
        return results.stream().filter(QueryResult::isSuccess)
                .mapToLong(QueryResult::getTotalClientTimeMs).sum();
    }
}
