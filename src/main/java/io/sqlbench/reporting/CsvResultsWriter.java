package io.sqlbench.reporting;

import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.util.CsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CsvResultsWriter {
    private static final Logger LOG = LoggerFactory.getLogger(CsvResultsWriter.class);
    private final String outputDir;

    public CsvResultsWriter(String outputDir) { this.outputDir = outputDir; }

    public void write(List<BenchmarkSuiteResult> allResults) throws IOException {
        for (BenchmarkSuiteResult sr : allResults) {
            String engineSlug = sr.getEngineName().toLowerCase().replaceAll("[^a-z0-9]", "-");
            String suiteSlug = sr.getSuiteName().toLowerCase().replaceAll("[^a-z0-9]", "-");

            for (BenchmarkRun run : sr.getRuns()) {
                Path path = Path.of(outputDir, engineSlug, suiteSlug,
                        "run-" + run.getRunNumber(), "results.csv");
                CsvUtils.writeResults(path, run.getRunNumber(), run.getResults());
                LOG.info("Written results → {}", path);
            }
        }
    }
}
