package io.sqlbench.runner;

import io.sqlbench.engine.QueryEngine;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.suite.SuiteDefinition;
import io.sqlbench.util.CsvUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class EngineRunner extends AbstractBenchmarkRunner {
    private final String runMode;
    private final String outputDir;
    private final String runTimestamp;

    public EngineRunner(QueryEngine engine, SuiteDefinition suite,
                        String runMode, int timeoutSeconds, int concurrentThreads,
                        String outputDir, String runTimestamp) {
        super(engine, suite, timeoutSeconds, concurrentThreads);
        this.runMode = runMode;
        this.outputDir = outputDir;
        this.runTimestamp = runTimestamp;
    }

    public BenchmarkSuiteResult runSuite() throws Exception {
        List<BenchmarkRun> runs = new ArrayList<>();

        String engineSlug = engine.getEngineName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        String suiteSlug  = suite.getName().toLowerCase().replaceAll("[^a-z0-9]", "-");

        for (int i = 1; i <= suite.getRunsPerSuite(); i++) {
            BenchmarkRun run = switch (runMode.toLowerCase()) {
                case "concurrent" -> runQueriesConcurrently(i);
                case "load-profile" -> {
                    if (suite.getLoadProfilePath() != null) yield runQueriesWithLoadProfile(i);
                    else { log.warn("No load profile set, falling back to sequential"); yield runQueriesSequentially(i); }
                }
                default -> runQueriesSequentially(i);
            };

            // Write results immediately after onRunFinished enrichment so query history
            // data is persisted before the next run starts (and before it can fall out
            // of the history window or be overwritten by the next run's queries).
            Path path = Path.of(outputDir, engineSlug, suiteSlug, runTimestamp, "run-" + run.getRunNumber(), "results.csv");
            CsvUtils.writeResults(path, run.getRunNumber(), run.getResults());
            log.info("Written results → {}", path);

            runs.add(run);
        }

        return new BenchmarkSuiteResult(engine.getEngineName(), suite.getName(), runs);
    }
}
