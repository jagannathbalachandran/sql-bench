package io.sqlbench.analysis;

public class DegradationResult {
    private final String queryAlias;
    private final long baselineMs;
    private final long currentMs;
    private final double changePercent;

    public DegradationResult(String queryAlias, long baselineMs, long currentMs) {
        this.queryAlias = queryAlias;
        this.baselineMs = baselineMs;
        this.currentMs = currentMs;
        this.changePercent = baselineMs > 0 ? ((double)(currentMs - baselineMs) / baselineMs) * 100 : 0;
    }

    public boolean isRegression(double thresholdPercent) { return changePercent > thresholdPercent; }
    public boolean isImprovement() { return changePercent < 0; }
    public String getQueryAlias() { return queryAlias; }
    public long getBaselineMs() { return baselineMs; }
    public long getCurrentMs() { return currentMs; }
    public double getChangePercent() { return changePercent; }
}
