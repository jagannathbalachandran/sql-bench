package io.sqlbench.engine;

import io.sqlbench.config.EngineConfig;
import io.sqlbench.model.BenchmarkRun;
import io.sqlbench.model.QueryDefinition;
import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractJdbcEngine implements QueryEngine {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected EngineConfig config;
    private String jdbcUrl;
    private Properties jdbcProps;

    // one connection per thread — thread-safe for concurrent runs
    private final ConcurrentHashMap<Long, Connection> connections = new ConcurrentHashMap<>();

    @Override
    public void connect(EngineConfig config) throws SQLException {
        this.config = config;
        this.jdbcUrl = config.getJdbc().getUrl();

        jdbcProps = new Properties();
        if (config.getJdbc().getUsername() != null)
            jdbcProps.put("user", config.getJdbc().getUsername());
        if (config.getJdbc().getPassword() != null)
            jdbcProps.put("password", config.getJdbc().getPassword());
        for (Map.Entry<String, String> e : config.getJdbc().getProperties().entrySet()) {
            jdbcProps.put(e.getKey(), e.getValue());
        }

        try {
            Class.forName(config.getJdbc().getDriverClass());
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + config.getJdbc().getDriverClass(), e);
        }

        // verify connection
        try (Connection c = DriverManager.getConnection(jdbcUrl, jdbcProps)) {
            log.info("Connected to {} ({})", config.getName(), jdbcUrl);
        }
    }

    protected Connection getConnection() throws SQLException {
        long tid = Thread.currentThread().getId();
        Connection conn = connections.get(tid);
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(jdbcUrl, jdbcProps);
                connections.put(tid, conn);
            }
        } catch (SQLException e) {
            conn = DriverManager.getConnection(jdbcUrl, jdbcProps);
            connections.put(tid, conn);
        }
        return conn;
    }

    @Override
    public QueryResult executeQuery(QueryDefinition query, Duration timeout) {
        long startMs = System.currentTimeMillis();
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout((int) timeout.getSeconds());
                long rowCount = 0;

                boolean hasResults = stmt.execute(query.getSql());
                if (hasResults) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        while (rs.next()) rowCount++;
                    }
                } else {
                    rowCount = stmt.getUpdateCount();
                }

                long totalMs = System.currentTimeMillis() - startMs;
                QueryResult result = QueryResult.success(getEngineName(), query.getAlias(), totalMs, rowCount);
                enrichFromStatement(result, stmt, query);
                return result;
            }
        } catch (SQLTimeoutException e) {
            return QueryResult.timedOut(getEngineName(), query.getAlias(), System.currentTimeMillis() - startMs);
        } catch (Exception e) {
            log.warn("Query {} failed: {}", query.getAlias(), e.getMessage());
            return QueryResult.failed(getEngineName(), query.getAlias(), System.currentTimeMillis() - startMs, e.getMessage());
        }
    }

    protected void enrichFromStatement(QueryResult result, Statement stmt, QueryDefinition query) throws SQLException {
        // subclasses may override to extract engine-specific metrics
    }

    @Override
    public void onRunFinished(BenchmarkRun run) {
        // subclasses may override for post-run metric enrichment (e.g. query history API)
    }

    @Override
    public String getEngineName() {
        return config != null ? config.getName() : "unknown";
    }

    @Override
    public void close() {
        connections.values().forEach(c -> {
            try { if (!c.isClosed()) c.close(); } catch (SQLException ignored) {}
        });
        connections.clear();
    }
}
