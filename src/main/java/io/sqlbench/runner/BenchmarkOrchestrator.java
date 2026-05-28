package io.sqlbench.runner;

import io.sqlbench.analysis.*;
import io.sqlbench.config.BenchmarkConfig;
import io.sqlbench.config.OutputConfig;
import io.sqlbench.engine.EngineFactory;
import io.sqlbench.engine.QueryEngine;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.reporting.CsvResultsWriter;
import io.sqlbench.reporting.ComparisonReporter;
import io.sqlbench.reporting.MarkdownReporter;
import io.sqlbench.reporting.SlackReporter;
import io.sqlbench.suite.SuiteLoader;
import io.sqlbench.suite.SuiteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BenchmarkOrchestrator {
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkOrchestrator.class);
    private final BenchmarkConfig config;

    public BenchmarkOrchestrator(BenchmarkConfig config) { this.config = config; }

    public void run() throws Exception {
        OutputConfig out = config.getOutput();
        List<QueryEngine> engines = new ArrayList<>();
        List<BenchmarkSuiteResult> allResults = new ArrayList<>();

        // Stage 1: connect to engines
        LOG.info("=== Connecting to engines ===");
        for (String enginePath : config.getEngines()) {
            QueryEngine engine = EngineFactory.create(enginePath);
            engines.add(engine);
            LOG.info("  Connected: {}", engine.getEngineName());
        }

        // Stage 2: run suites for each engine
        LOG.info("=== Running benchmarks ===");
        for (QueryEngine engine : engines) {
            for (String suitePath : config.getSuites()) {
                SuiteDefinition suite = SuiteLoader.load(suitePath, engine.getEngineName());
                LOG.info("Running suite '{}' on '{}'", suite.getName(), engine.getEngineName());

                EngineRunner runner = new EngineRunner(
                        engine, suite,
                        config.getRunMode(),
                        config.getQueryTimeoutSeconds(),
                        config.getConcurrentThreads());

                BenchmarkSuiteResult result = runner.runSuite();
                allResults.add(result);
            }
        }

        // Stage 3: select best runs
        LOG.info("=== Selecting best runs ===");
        for (BenchmarkSuiteResult r : allResults) {
            ValidationStrategies strategy = ValidationStrategies.from(
                    findSuiteValidation(r.getSuiteName()));
            int best = strategy.selectBestRunIndex(r);
            r.setBestRunIndex(best);
            LOG.info("  {} / {} → best run: #{}", r.getEngineName(), r.getSuiteName(), best + 1);
        }

        // Stage 4: write CSV results
        LOG.info("=== Writing CSV results ===");
        new CsvResultsWriter(out.getLocalDir()).write(allResults);

        // Stage 5: regression analysis (per engine vs local baseline)
        LOG.info("=== Running degradation analysis ===");
        BaselineManager baselineMgr = out.isUploadToS3()
                ? new S3BaselineManager(out.getS3Bucket(), out.getS3Prefix())
                : new LocalBaselineManager(out.getLocalDir());

        List<DegradationResult> allRegressions = new ArrayList<>();
        for (BenchmarkSuiteResult r : allResults) {
            Map<String, Long> baseline = baselineMgr.loadBaseline(r.getSuiteName() + "-" + r.getEngineName());
            List<DegradationResult> regressions = new DegradationAnalyzer()
                    .analyze(r.getBestRun().getResults(), baseline);
            allRegressions.addAll(regressions);
            baselineMgr.saveBaseline(r.getSuiteName() + "-" + r.getEngineName(),
                    r.getBestRun().getResults());
        }

        // Stage 6: generate Markdown report + comparisons
        LOG.info("=== Generating report ===");
        String firstEngineName = engines.isEmpty() ? "" : engines.get(0).getEngineName();
        List<ComparisonReporter.EngineComparison> comparisons =
                ComparisonReporter.compare(firstEngineName, allResults);

        new MarkdownReporter(out.getLocalDir()).generate(allResults, allRegressions, comparisons);

        // Stage 7: optional Slack notification
        if (out.isSlackEnabled() && out.getSlackWebhook() != null) {
            LOG.info("=== Sending Slack notification ===");
            new SlackReporter(out.getSlackWebhook()).post(allResults, comparisons);
        }

        // Close all engine connections
        engines.forEach(QueryEngine::close);

        LOG.info("=== Benchmark complete. Results in: {} ===", out.getLocalDir());

        // Fail fast if there are regressions
        if (!allRegressions.isEmpty()) {
            LOG.warn("{} regression(s) detected!", allRegressions.size());
        }
    }

    private String findSuiteValidation(String suiteName) {
        // default to best-run; could extend to look up from suite config list
        return "best-run";
    }
}
