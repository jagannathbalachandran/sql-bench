package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EngineConfig {
    private String name;
    private String type;   // custom | databricks | snowflake
    private JdbcConfig jdbc = new JdbcConfig();
    private ConnectionConfig connection = new ConnectionConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JdbcConfig {
        private String driverClass;
        private String url;
        private String username;
        private String password;
        private Map<String, String> properties = new HashMap<>();

        public String getDriverClass() { return driverClass; }
        public void setDriverClass(String driverClass) { this.driverClass = driverClass; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Map<String, String> getProperties() { return properties; }
        public void setProperties(Map<String, String> properties) { this.properties = properties; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConnectionConfig {
        private int poolSize = 5;
        private int timeoutSeconds = 420;

        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public JdbcConfig getJdbc() { return jdbc; }
    public void setJdbc(JdbcConfig jdbc) { this.jdbc = jdbc; }
    public ConnectionConfig getConnection() { return connection; }
    public void setConnection(ConnectionConfig connection) { this.connection = connection; }
}
