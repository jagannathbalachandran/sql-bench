package io.sqlbench.reporting;

import io.sqlbench.analysis.DegradationResult;
import io.sqlbench.model.BenchmarkMetrics;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.model.QueryResult;
import io.sqlbench.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MarkdownReporter {
    private static final Logger LOG = LoggerFactory.getLogger(MarkdownReporter.class);
    private final String outputDir;

    public MarkdownReporter(String outputDir) { this.outputDir = outputDir; }

    public void generate(List<BenchmarkSuiteResult> results,
                         List<DegradationResult> regressions,
                         List<ComparisonReporter.EngineComparison> comparisons) throws IOException {
        Path reportPath = Path.of(outputDir, "report.md");
        Files.createDirectories(Path.of(outputDir));

        StringBuilder md = new StringBuilder();
        md.append("# SQL Benchmark Report\n\n");
        md.append("Generated: ").append(TimeUtils.nowIso()).append("\n\n");

        // Summary table
        md.append("## Engine Summary\n\n");
        md.append("| Engine | Suite | Queries | Success | Failed | Total Time | p50 | p95 |\n");
        md.append("|--------|-------|---------|---------|--------|------------|-----|-----|\n");
        for (BenchmarkSuiteResult sr : results) {
            BenchmarkMetrics m = sr.getBestRun().computeMetrics();
            md.append(String.format("| %s | %s | %d | %d | %d | %s | %s | %s |\n",
                    sr.getEngineName(), sr.getSuiteName(),
                    m.getQueryCount(), m.getSuccessCount(), m.getFailureCount(),
                    TimeUtils.formatDuration(m.getTotalTimeMs()),
                    TimeUtils.formatDuration(m.getP50Ms()),
                    TimeUtils.formatDuration(m.getP95Ms())));
        }

        // Win/loss comparisons
        if (!comparisons.isEmpty()) {
            md.append("\n## Head-to-Head Comparison\n\n");
            md.append("| vs | Wins | Losses | Ties | Avg (base) | Avg (vs) | Base Faster By |\n");
            md.append("|----|------|--------|------|------------|----------|----------------|\n");
            for (ComparisonReporter.EngineComparison c : comparisons) {
                md.append(String.format("| %s vs %s | %d | %d | %d | %s | %s | %+.1f%% |\n",
                        c.baseEngine(), c.compareEngine(),
                        c.baseWins(), c.compareWins(), c.ties(),
                        TimeUtils.formatDuration((long) c.baseAvgMs()),
                        TimeUtils.formatDuration((long) c.compareAvgMs()),
                        c.baseSpeedupPercent()));
            }
        }

        // Per-query delta table (best run of each engine, side by side)
        if (results.size() > 1) {
            md.append("\n## Per-Query Times (ms) — Best Run\n\n");
            List<String> engineNames = results.stream().map(BenchmarkSuiteResult::getEngineName).toList();
            md.append("| Query |");
            engineNames.forEach(e -> md.append(" ").append(e).append(" |"));
            md.append("\n|-------|");
            engineNames.forEach(e -> md.append("-------|"));
            md.append("\n");

            // collect all aliases
            Set<String> aliases = new LinkedHashSet<>();
            results.forEach(sr -> sr.getBestRun().getResults().stream()
                    .filter(QueryResult::isSuccess)
                    .map(QueryResult::getQueryAlias)
                    .forEach(aliases::add));

            Map<String, Map<String, Long>> byEngine = new LinkedHashMap<>();
            for (BenchmarkSuiteResult sr : results) {
                Map<String, Long> times = new LinkedHashMap<>();
                sr.getBestRun().getResults().stream()
                        .filter(QueryResult::isSuccess)
                        .forEach(r -> times.put(r.getQueryAlias(), r.getTotalClientTimeMs()));
                byEngine.put(sr.getEngineName(), times);
            }

            for (String alias : aliases) {
                md.append("| ").append(alias).append(" |");
                for (String eng : engineNames) {
                    Long t = byEngine.getOrDefault(eng, Map.of()).get(alias);
                    md.append(t != null ? " " + t + " |" : " — |");
                }
                md.append("\n");
            }
        }

        // Regressions
        if (!regressions.isEmpty()) {
            md.append("\n## Regressions vs Baseline\n\n");
            md.append("| Query | Baseline (ms) | Current (ms) | Change |\n");
            md.append("|-------|---------------|--------------|--------|\n");
            for (DegradationResult r : regressions) {
                md.append(String.format("| %s | %d | %d | %+.1f%% |\n",
                        r.getQueryAlias(), r.getBaselineMs(), r.getCurrentMs(), r.getChangePercent()));
            }
        }

        Files.writeString(reportPath, md.toString());
        LOG.info("Markdown report written to {}", reportPath);
    }
}
