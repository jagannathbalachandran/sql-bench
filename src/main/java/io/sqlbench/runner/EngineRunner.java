package io.sqlbench.runner;

import io.sqlbench.engine.QueryEngine;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.suite.SuiteDefinition;

import java.util.ArrayList;
import java.util.List;

public class EngineRunner extends AbstractBenchmarkRunner {
    private final String runMode;

    public EngineRunner(QueryEngine engine, SuiteDefinition suite,
                        String runMode, int timeoutSeconds, int concurrentThreads) {
        super(engine, suite, timeoutSeconds, concurrentThreads);
        this.runMode = runMode;
    }

    public BenchmarkSuiteResult runSuite() throws Exception {
        List<BenchmarkRun> runs = new ArrayList<>();

        for (int i = 1; i <= suite.getRunsPerSuite(); i++) {
            BenchmarkRun run = switch (runMode.toLowerCase()) {
                case "concurrent" -> runQueriesConcurrently(i);
                case "load-profile" -> {
                    if (suite.getLoadProfilePath() != null) yield runQueriesWithLoadProfile(i);
                    else { log.warn("No load profile set, falling back to sequential"); yield runQueriesSequentially(i); }
                }
                default -> runQueriesSequentially(i);
            };
            runs.add(run);
        }

        return new BenchmarkSuiteResult(engine.getEngineName(), suite.getName(), runs);
    }
}
