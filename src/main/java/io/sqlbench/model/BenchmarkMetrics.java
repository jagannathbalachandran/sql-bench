package io.sqlbench.model;

public class BenchmarkMetrics {
    private final long p50Ms;
    private final long p75Ms;
    private final long p90Ms;
    private final long p95Ms;
    private final long p99Ms;
    private final long totalTimeMs;
    private final int queryCount;
    private final int successCount;
    private final int failureCount;

    public BenchmarkMetrics(long p50Ms, long p75Ms, long p90Ms, long p95Ms, long p99Ms,
                            long totalTimeMs, int queryCount, int successCount, int failureCount) {
        this.p50Ms = p50Ms;
        this.p75Ms = p75Ms;
        this.p90Ms = p90Ms;
        this.p95Ms = p95Ms;
        this.p99Ms = p99Ms;
        this.totalTimeMs = totalTimeMs;
        this.queryCount = queryCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public long getP50Ms() { return p50Ms; }
    public long getP75Ms() { return p75Ms; }
    public long getP90Ms() { return p90Ms; }
    public long getP95Ms() { return p95Ms; }
    public long getP99Ms() { return p99Ms; }
    public long getTotalTimeMs() { return totalTimeMs; }
    public int getQueryCount() { return queryCount; }
    public int getSuccessCount() { return successCount; }
    public int getFailureCount() { return failureCount; }
}
