package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabricksEngineConfig extends EngineConfig {
    private DatabricksConfig databricks = new DatabricksConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DatabricksConfig {
        private boolean fetchQueryHistory = true;
        private String historyFetchMode = "api";   // api | sql
        private String apiToken;
        private String apiHost;
        private int retryCount = 3;

        public boolean isFetchQueryHistory() { return fetchQueryHistory; }
        public void setFetchQueryHistory(boolean v) { this.fetchQueryHistory = v; }
        public String getHistoryFetchMode() { return historyFetchMode; }
        public void setHistoryFetchMode(String v) { this.historyFetchMode = v; }
        public String getApiToken() { return apiToken; }
        public void setApiToken(String v) { this.apiToken = v; }
        public String getApiHost() { return apiHost; }
        public void setApiHost(String v) { this.apiHost = v; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int v) { this.retryCount = v; }
    }

    public DatabricksConfig getDatabricks() { return databricks; }
    public void setDatabricks(DatabricksConfig v) { this.databricks = v; }
}
