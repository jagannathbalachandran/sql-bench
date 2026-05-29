package io.sqlbench.engine;

import io.sqlbench.config.SnowflakeEngineConfig;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SnowflakeEngine extends AbstractJdbcEngine {
    private static final Logger LOG = LoggerFactory.getLogger(SnowflakeEngine.class);

    @Override
    public void onRunFinished(BenchmarkRun run) {
        if (!(config instanceof SnowflakeEngineConfig cfg)) return;
        if (!cfg.getSnowflake().isFetchQueryHistory()) return;

        long startMs = run.getStartTimeMs() - 5000;
        long endMs = run.getEndTimeMs() + 5000;

        String sql = String.format(
            "SELECT query_id, query_text, compilation_time, execution_time " +
            "FROM TABLE(INFORMATION_SCHEMA.QUERY_HISTORY(" +
            "END_TIME_RANGE_START => TO_TIMESTAMP_LTZ(%d, 3), " +
            "END_TIME_RANGE_END   => TO_TIMESTAMP_LTZ(%d, 3))) " +
            "ORDER BY start_time DESC LIMIT 500", startMs, endMs);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String queryId = rs.getString("QUERY_ID");
                String queryText = rs.getString("QUERY_TEXT");
                long compilationMs = rs.getLong("COMPILATION_TIME");
                long executionMs = rs.getLong("EXECUTION_TIME");

                for (QueryResult r : run.getResults()) {
                    if (queryText != null && queryText.contains(r.getQueryAlias())) {
                        r.setQueryId(queryId);
                        r.setPlanningTimeMs(compilationMs);
                        r.setExecutionTimeMs(executionMs);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not enrich Snowflake query history: {}", e.getMessage());
        }
    }
}
