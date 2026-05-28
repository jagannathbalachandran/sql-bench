package io.sqlbench.engine;

import io.sqlbench.config.EngineConfig;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.QueryDefinition;
import io.sqlbench.model.QueryResult;

import java.sql.SQLException;
import java.time.Duration;

public interface QueryEngine extends AutoCloseable {
    void connect(EngineConfig config) throws SQLException;
    QueryResult executeQuery(QueryDefinition query, Duration timeout);
    void onRunFinished(BenchmarkRun run);   // hook for post-run metric enrichment
    String getEngineName();
    void close();
}
