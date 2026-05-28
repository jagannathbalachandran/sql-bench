package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SnowflakeEngineConfig extends EngineConfig {
    private SnowflakeConfig snowflake = new SnowflakeConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SnowflakeConfig {
        private boolean fetchQueryHistory = true;
        private String reportingWarehouse;

        public boolean isFetchQueryHistory() { return fetchQueryHistory; }
        public void setFetchQueryHistory(boolean v) { this.fetchQueryHistory = v; }
        public String getReportingWarehouse() { return reportingWarehouse; }
        public void setReportingWarehouse(String v) { this.reportingWarehouse = v; }
    }

    public SnowflakeConfig getSnowflake() { return snowflake; }
    public void setSnowflake(SnowflakeConfig v) { this.snowflake = v; }
}
