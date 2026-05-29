package io.sqlbench.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqlbench.config.DatabricksEngineConfig;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DatabricksEngine extends AbstractJdbcEngine {
    private static final Logger LOG = LoggerFactory.getLogger(DatabricksEngine.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    @Override
    public void onRunFinished(BenchmarkRun run) {
        if (!(config instanceof DatabricksEngineConfig cfg)) return;
        if (!cfg.getDatabricks().isFetchQueryHistory()) return;

        int delaySecs = cfg.getDatabricks().getQueryHistoryDelaySeconds();
        if (delaySecs > 0) {
            LOG.info("Waiting {}s for Databricks query history to be available...", delaySecs);
            try { Thread.sleep(delaySecs * 1000L); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        if ("sql".equalsIgnoreCase(cfg.getDatabricks().getHistoryFetchMode())) {
            enrichViaSql(run);
        } else {
            enrichViaApi(run, cfg);
        }

        for (QueryResult r : run.getResults()) {
            if (r.isSuccess() && r.getPlanningTimeMs() == -1) {
                LOG.warn("No Databricks query history match for '{}' — compilation/execution times unavailable", r.getQueryAlias());
            }
        }
    }

    private void enrichViaSql(BenchmarkRun run) {
        long startMs = run.getStartTimeMs() - 5000;
        long endMs = run.getEndTimeMs() + 5000;
        String sql = String.format(
            "SELECT statement_id, statement_text, compilation_time_ms, execution_time_ms " +
            "FROM system.query.history " +
            "WHERE start_time BETWEEN TIMESTAMP_MILLIS(%d) AND TIMESTAMP_MILLIS(%d) " +
            "AND executed_by = CURRENT_USER()", startMs, endMs);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            record HistoryEntry(String id, String normalizedText, long planMs, long execMs) {}
            java.util.List<HistoryEntry> entries = new java.util.ArrayList<>();
            while (rs.next()) {
                entries.add(new HistoryEntry(
                    rs.getString("statement_id"),
                    normalizeQuery(rs.getString("statement_text")),
                    rs.getLong("compilation_time_ms"),
                    rs.getLong("execution_time_ms")
                ));
            }

            for (QueryResult r : run.getResults()) {
                if (r.getQuerySql() == null) continue;
                String prefix = normalizeQuery(r.getQuerySql());
                prefix = prefix.substring(0, Math.min(200, prefix.length()));
                for (HistoryEntry e : entries) {
                    if (e.normalizedText().startsWith(prefix)) {
                        r.setQueryId(e.id());
                        r.setPlanningTimeMs(e.planMs());
                        r.setExecutionTimeMs(e.execMs());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not enrich DBR metrics via SQL: {}", e.getMessage());
        }
    }

    private void enrichViaApi(BenchmarkRun run, DatabricksEngineConfig cfg) {
        String host = cfg.getDatabricks().getApiHost();
        String token = cfg.getDatabricks().getApiToken();
        if (host == null || token == null) return;

        long startMs = run.getStartTimeMs() - 5000;
        long endMs = run.getEndTimeMs() + 5000;
        String url = String.format("https://%s/api/2.0/sql/history/queries" +
                "?filter_by.query_start_time_range.start_time_ms=%d" +
                "&filter_by.query_start_time_range.end_time_ms=%d" +
                "&max_results=100", host, startMs, endMs);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                LOG.warn("DBR query history API returned {}", resp.statusCode());
                return;
            }

            JsonNode body = MAPPER.readTree(resp.body());
            JsonNode queryList = body.path("res");
            Map<String, QueryResult> resultsByAlias = new HashMap<>();
            run.getResults().forEach(r -> resultsByAlias.put(r.getQueryAlias(), r));

            for (JsonNode q : queryList) {
                String normalizedHistory = normalizeQuery(q.path("query_text").asText());
                long planMs = q.path("compilation_time_ms").asLong(-1);
                long execMs = q.path("execution_time_ms").asLong(-1);
                String queryId = q.path("query_id").asText();

                for (QueryResult r : run.getResults()) {
                    if (r.getQuerySql() == null) continue;
                    String normalizedQuery = normalizeQuery(r.getQuerySql());
                    int prefixLen = Math.min(200, normalizedQuery.length());
                    if (normalizedHistory.startsWith(normalizedQuery.substring(0, prefixLen))) {
                        r.setQueryId(queryId);
                        r.setPlanningTimeMs(planMs);
                        r.setExecutionTimeMs(execMs);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not enrich DBR metrics via API: {}", e.getMessage());
        }
    }

    private String normalizeQuery(String q) {
        if (q == null) return "";
        return q.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
