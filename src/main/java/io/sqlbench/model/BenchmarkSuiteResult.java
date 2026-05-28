package io.sqlbench.model;

import java.util.List;

public class BenchmarkSuiteResult {
    private final String engineName;
    private final String suiteName;
    private final List<BenchmarkRun> runs;
    private int bestRunIndex = 0;

    public BenchmarkSuiteResult(String engineName, String suiteName, List<BenchmarkRun> runs) {
        this.engineName = engineName;
        this.suiteName = suiteName;
        this.runs = runs;
    }

    public BenchmarkRun getBestRun() {
        return runs.get(bestRunIndex);
    }

    public String getEngineName() { return engineName; }
    public String getSuiteName() { return suiteName; }
    public List<BenchmarkRun> getRuns() { return runs; }
    public int getBestRunIndex() { return bestRunIndex; }
    public void setBestRunIndex(int bestRunIndex) { this.bestRunIndex = bestRunIndex; }
}
