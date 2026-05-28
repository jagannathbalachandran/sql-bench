package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomEngineConfig extends EngineConfig {
    private MetricsConfig metrics = new MetricsConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricsConfig {
        private String explainJsonColumn;
        private String planningTimeMsColumn;

        public String getExplainJsonColumn() { return explainJsonColumn; }
        public void setExplainJsonColumn(String v) { this.explainJsonColumn = v; }
        public String getPlanningTimeMsColumn() { return planningTimeMsColumn; }
        public void setPlanningTimeMsColumn(String v) { this.planningTimeMsColumn = v; }
    }

    public MetricsConfig getMetrics() { return metrics; }
    public void setMetrics(MetricsConfig metrics) { this.metrics = metrics; }
}
