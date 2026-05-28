package io.sqlbench.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.sqlbench.model.BenchmarkMetrics;
import io.sqlbench.model.BenchmarkSuiteResult;
import io.sqlbench.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class SlackReporter {
    private static final Logger LOG = LoggerFactory.getLogger(SlackReporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String webhookUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SlackReporter(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public void post(List<BenchmarkSuiteResult> results,
                     List<ComparisonReporter.EngineComparison> comparisons) {
        try {
            ObjectNode payload = MAPPER.createObjectNode();
            ArrayNode blocks = payload.putArray("blocks");

            // header
            ObjectNode header = blocks.addObject();
            header.put("type", "header");
            header.putObject("text").put("type", "plain_text").put("text", "SQL Benchmark Results");

            // summary section
            StringBuilder summary = new StringBuilder();
            for (BenchmarkSuiteResult sr : results) {
                BenchmarkMetrics m = sr.getBestRun().computeMetrics();
                summary.append(String.format("*%s* — %s total, p50 %s, p95 %s (%d/%d success)\n",
                        sr.getEngineName(),
                        TimeUtils.formatDuration(m.getTotalTimeMs()),
                        TimeUtils.formatDuration(m.getP50Ms()),
                        TimeUtils.formatDuration(m.getP95Ms()),
                        m.getSuccessCount(), m.getQueryCount()));
            }
            ObjectNode section = blocks.addObject();
            section.put("type", "section");
            section.putObject("text").put("type", "mrkdwn").put("text", summary.toString().trim());

            // comparisons
            if (!comparisons.isEmpty()) {
                StringBuilder cmpText = new StringBuilder("*Head-to-Head:*\n");
                for (ComparisonReporter.EngineComparison c : comparisons) {
                    cmpText.append(String.format("• %s vs %s: %d wins / %d losses (%+.1f%%)\n",
                            c.baseEngine(), c.compareEngine(),
                            c.baseWins(), c.compareWins(), c.baseSpeedupPercent()));
                }
                ObjectNode cmpBlock = blocks.addObject();
                cmpBlock.put("type", "section");
                cmpBlock.putObject("text").put("type", "mrkdwn").put("text", cmpText.toString().trim());
            }

            String json = MAPPER.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                LOG.info("Slack notification sent");
            } else {
                LOG.warn("Slack returned status {}: {}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            LOG.warn("Failed to send Slack notification: {}", e.getMessage());
        }
    }
}
