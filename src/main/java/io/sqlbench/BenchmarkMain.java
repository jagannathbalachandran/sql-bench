package io.sqlbench;

import io.sqlbench.config.BenchmarkConfig;
import io.sqlbench.config.ConfigLoader;
import io.sqlbench.runner.BenchmarkOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchmarkMain {
    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkMain.class);

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : "configs/benchmark.yml";
        LOG.info("sql-bench starting. Config: {}", configPath);
        try {
            BenchmarkConfig config = ConfigLoader.load(configPath);
            new BenchmarkOrchestrator(config).run();
        } catch (Exception e) {
            LOG.error("Benchmark failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
