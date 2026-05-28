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

        if ("sql".equalsIgnoreCase(cfg.getDatabricks().getHistoryFetchMode())) {
            enrichViaSql(run);
        } else {
            enrichViaApi(run, cfg);
        }
    }

    private void enrichViaSql(BenchmarkRun run) {
        long startMs = run.getStartTimeMs() - 5000;
        long endMs = run.getEndTimeMs() + 5000;
        String sql = String.format(
            "SELECT statement_id, statement_text, execution_status, " +
            "total_duration_ms, compilation_time_ms, execution_time_ms " +
            "FROM system.query.history " +
            "WHERE start_time BETWEEN TIMESTAMP_MILLIS(%d) AND TIMESTAMP_MILLIS(%d) " +
            "AND executed_by = CURRENT_USER()", startMs, endMs);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Map<String, long[]> historyMap = new HashMap<>();
            while (rs.next()) {
                String text = rs.getString("statement_text");
                long planMs = rs.getLong("compilation_time_ms");
                long execMs = rs.getLong("execution_time_ms");
                historyMap.put(normalizeQuery(text), new long[]{planMs, execMs});
            }

            for (QueryResult r : run.getResults()) {
                long[] times = historyMap.get(normalizeQuery(r.getQueryAlias()));
                if (times != null) {
                    r.setPlanningTimeMs(times[0]);
                    r.setExecutionTimeMs(times[1]);
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
                String queryText = q.path("query_text").asText();
                long planMs = q.path("compilation_time_ms").asLong(-1);
                long execMs = q.path("execution_time_ms").asLong(-1);
                String queryId = q.path("query_id").asText();

                for (QueryResult r : run.getResults()) {
                    if (queryText.contains(r.getQueryAlias()) || r.getQueryAlias().contains(normalizeQuery(queryText))) {
                        r.setPlanningTimeMs(planMs);
                        r.setExecutionTimeMs(execMs);
                        r.setQueryId(queryId);
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
