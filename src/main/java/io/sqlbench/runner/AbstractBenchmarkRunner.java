package io.sqlbench.runner;

import io.sqlbench.engine.QueryEngine;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.QueryDefinition;
import io.sqlbench.model.QueryResult;
import io.sqlbench.suite.SuiteDefinition;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractBenchmarkRunner {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final QueryEngine engine;
    protected final SuiteDefinition suite;
    protected final int timeoutSeconds;
    protected final int concurrentThreads;

    protected AbstractBenchmarkRunner(QueryEngine engine, SuiteDefinition suite,
                                       int timeoutSeconds, int concurrentThreads) {
        this.engine = engine;
        this.suite = suite;
        this.timeoutSeconds = timeoutSeconds;
        this.concurrentThreads = concurrentThreads;
    }

    protected BenchmarkRun runQueriesSequentially(int runNumber) {
        BenchmarkRun run = new BenchmarkRun(runNumber);
        List<QueryDefinition> queries = suite.getQueries();
        log.info("[{}] Run {}/{} — sequential, {} queries",
                engine.getEngineName(), runNumber, suite.getRunsPerSuite(), queries.size());

        for (QueryDefinition q : queries) {
            QueryResult result = engine.executeQuery(q, Duration.ofSeconds(timeoutSeconds));
            run.addResult(result);
            logResult(result);
        }
        run.finish();
        engine.onRunFinished(run);
        return run;
    }

    protected BenchmarkRun runQueriesConcurrently(int runNumber) {
        BenchmarkRun run = new BenchmarkRun(runNumber);
        List<QueryDefinition> queries = suite.getQueries();
        log.info("[{}] Run {}/{} — concurrent ({} threads), {} queries",
                engine.getEngineName(), runNumber, suite.getRunsPerSuite(), concurrentThreads, queries.size());

        ExecutorService pool = Executors.newFixedThreadPool(concurrentThreads);
        List<Future<QueryResult>> futures = new ArrayList<>();

        for (QueryDefinition q : queries) {
            futures.add(pool.submit(() -> engine.executeQuery(q, Duration.ofSeconds(timeoutSeconds))));
        }

        pool.shutdown();
        try { pool.awaitTermination(timeoutSeconds * 2L, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        for (Future<QueryResult> f : futures) {
            try {
                QueryResult r = f.get();
                run.addResult(r);
                logResult(r);
            } catch (Exception e) {
                log.error("Future failed: {}", e.getMessage());
            }
        }
        run.finish();
        engine.onRunFinished(run);
        return run;
    }

    protected BenchmarkRun runQueriesWithLoadProfile(int runNumber) throws Exception {
        BenchmarkRun run = new BenchmarkRun(runNumber);
        List<QueryDefinition> queries = suite.getQueries();
        String profilePath = suite.getLoadProfilePath();

        log.info("[{}] Run {}/{} — load profile ({}), {} queries",
                engine.getEngineName(), runNumber, suite.getRunsPerSuite(), profilePath, queries.size());

        List<long[]> schedule = loadProfile(profilePath);   // [timeSeconds, qps]
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(concurrentThreads);
        ExecutorService pool = Executors.newFixedThreadPool(concurrentThreads);
        List<Future<QueryResult>> futures = new CopyOnWriteArrayList<>();

        int queryIndex = 0;
        for (long[] slot : schedule) {
            long delaySecs = slot[0];
            long qps = slot[1];
            for (int i = 0; i < qps; i++) {
                final QueryDefinition q = queries.get(queryIndex % queries.size());
                queryIndex++;
                final long finalDelay = delaySecs * 1000 + (i * (1000 / Math.max(1, qps)));
                scheduler.schedule(() -> {
                    futures.add(pool.submit(() -> engine.executeQuery(q, Duration.ofSeconds(timeoutSeconds))));
                }, finalDelay, TimeUnit.MILLISECONDS);
            }
        }

        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.MINUTES);
        pool.shutdown();
        pool.awaitTermination(timeoutSeconds * 2L, TimeUnit.SECONDS);

        for (Future<QueryResult> f : futures) {
            try { run.addResult(f.get()); } catch (Exception ignored) {}
        }
        run.finish();
        engine.onRunFinished(run);
        return run;
    }

    private List<long[]> loadProfile(String profilePath) throws Exception {
        List<long[]> schedule = new ArrayList<>();
        try (Reader r = Files.newBufferedReader(Path.of(profilePath));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).build().parse(r)) {
            for (CSVRecord rec : parser) {
                long timeSecs = Long.parseLong(rec.get("time_seconds").trim());
                long qps = Long.parseLong(rec.get("qps").trim());
                schedule.add(new long[]{timeSecs, qps});
            }
        }
        return schedule;
    }

    private void logResult(QueryResult r) {
        if (r.isSuccess()) {
            log.info("  [OK] {} — {}ms ({} rows)", r.getQueryAlias(), r.getTotalClientTimeMs(), r.getRowCount());
        } else {
            log.warn("  [{}] {} — {}ms — {}", r.getStatus(), r.getQueryAlias(),
                    r.getTotalClientTimeMs(), r.getErrorMessage());
        }
    }
}
