package io.sqlbench.engine;

import io.sqlbench.config.CustomEngineConfig;
import io.sqlbench.model.QueryDefinition;
import io.sqlbench.model.QueryResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomEngine extends AbstractJdbcEngine {

    @Override
    protected void enrichFromStatement(QueryResult result, Statement stmt, QueryDefinition query) throws SQLException {
        if (!(config instanceof CustomEngineConfig cfg)) return;

        String planningCol = cfg.getMetrics().getPlanningTimeMsColumn();
        if (planningCol != null && !planningCol.isBlank()) {
            try (ResultSet rs = stmt.executeQuery("SELECT " + planningCol)) {
                if (rs.next()) result.setPlanningTimeMs(rs.getLong(1));
            } catch (SQLException ignored) {}
        }
    }
}
