package io.sqlbench.analysis;

import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;

import java.util.List;

public enum ValidationStrategies {
    BEST_RUN {
        @Override
        public int selectBestRunIndex(BenchmarkSuiteResult result) {
            List<BenchmarkRun> runs = result.getRuns();
            int best = 0;
            long bestTime = Long.MAX_VALUE;
            for (int i = 0; i < runs.size(); i++) {
                long t = runs.get(i).getTotalSuccessTime();
                if (t < bestTime) { bestTime = t; best = i; }
            }
            return best;
        }
    },
    SECOND_RUN {
        @Override
        public int selectBestRunIndex(BenchmarkSuiteResult result) {
            return Math.min(1, result.getRuns().size() - 1);
        }
    },
    NONE {
        @Override
        public int selectBestRunIndex(BenchmarkSuiteResult result) {
            return result.getRuns().size() - 1;
        }
    };

    public abstract int selectBestRunIndex(BenchmarkSuiteResult result);

    public static ValidationStrategies from(String name) {
        return switch (name.toLowerCase().replace("-", "_")) {
            case "best_run" -> BEST_RUN;
            case "second_run" -> SECOND_RUN;
            default -> NONE;
        };
    }
}
