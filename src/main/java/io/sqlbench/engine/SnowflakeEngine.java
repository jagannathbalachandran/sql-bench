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

        int delaySecs = cfg.getSnowflake().getQueryHistoryDelaySeconds();
        if (delaySecs > 0) {
            LOG.info("Waiting {}s for Snowflake query history to be available...", delaySecs);
            try { Thread.sleep(delaySecs * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        long startMs = run.getStartTimeMs() - 5000;
        long endMs = run.getEndTimeMs() + 5000;

        String sql = String.format(
            "SELECT query_id, query_text, compilation_time, execution_time " +
            "FROM TABLE(INFORMATION_SCHEMA.QUERY_HISTORY(" +
            "END_TIME_RANGE_START => TO_TIMESTAMP_LTZ(%d, 3), " +
            "END_TIME_RANGE_END   => TO_TIMESTAMP_LTZ(%d, 3))) " +
            "ORDER BY start_time DESC LIMIT 500", startMs, endMs);

        LOG.info("[QueryHistory] Fetching history for run {} — window [{}, {}]", run.getRunNumber(), startMs, endMs);
        LOG.debug("[QueryHistory] SQL: {}", sql);

        int historyRowCount = 0;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                historyRowCount++;
                String queryId = rs.getString("QUERY_ID");
                String queryText = rs.getString("QUERY_TEXT");
                long compilationMs = rs.getLong("COMPILATION_TIME");
                long executionMs = rs.getLong("EXECUTION_TIME");

                LOG.debug("[QueryHistory] Row {}: queryId={} compilationMs={} executionMs={} textPrefix='{}'",
                        historyRowCount, queryId, compilationMs, executionMs,
                        queryText != null ? queryText.substring(0, Math.min(80, queryText.length())) : "null");

                if (queryText == null) {
                    LOG.debug("[QueryHistory] Skipping row {} — null query text", historyRowCount);
                    continue;
                }
                String normalizedHistory = normalizeQuery(queryText);

                boolean matched = false;
                for (QueryResult r : run.getResults()) {
                    if (r.getQuerySql() == null) continue;
                    String normalizedQuery = normalizeQuery(r.getQuerySql());
                    int prefixLen = Math.min(200, normalizedQuery.length());
                    String expectedPrefix = normalizedQuery.substring(0, prefixLen);
                    String actualPrefix = normalizedHistory.substring(0, Math.min(prefixLen, normalizedHistory.length()));
                    if (normalizedHistory.startsWith(expectedPrefix)) {
                        LOG.info("[QueryHistory] Matched '{}' → queryId={} compilation={}ms execution={}ms",
                                r.getQueryAlias(), queryId, compilationMs, executionMs);
                        r.setQueryId(queryId);
                        r.setPlanningTimeMs(compilationMs);
                        r.setExecutionTimeMs(executionMs);
                        matched = true;
                        break;
                    } else if (r.getQueryId().isEmpty()) {
                        LOG.debug("[QueryHistory] No match for '{}': expected prefix='{}...' got='{}...'",
                                r.getQueryAlias(), expectedPrefix.substring(0, Math.min(60, expectedPrefix.length())),
                                actualPrefix.substring(0, Math.min(60, actualPrefix.length())));
                    }
                }
                if (!matched) {
                    LOG.debug("[QueryHistory] Row {} did not match any benchmark query", historyRowCount);
                }
            }
        } catch (Exception e) {
            LOG.warn("[QueryHistory] Failed to fetch/enrich query history: {} — {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }

        LOG.info("[QueryHistory] Processed {} history row(s) for run {}", historyRowCount, run.getRunNumber());

        for (QueryResult r : run.getResults()) {
            if (r.isSuccess() && r.getPlanningTimeMs() == -1) {
                LOG.warn("[QueryHistory] No match found for '{}' — planning/execution times unavailable", r.getQueryAlias());
            }
        }
    }

    private static String normalizeQuery(String sql) {
        return sql.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
