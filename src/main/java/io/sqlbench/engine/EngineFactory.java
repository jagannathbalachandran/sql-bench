package io.sqlbench.engine;

import io.sqlbench.config.ConfigLoader;
import io.sqlbench.config.DatabricksEngineConfig;
import io.sqlbench.config.EngineConfig;
import io.sqlbench.config.SnowflakeEngineConfig;

import java.io.IOException;
import java.sql.SQLException;

public class EngineFactory {

    public static QueryEngine create(String engineConfigPath) throws IOException, SQLException {
        EngineConfig cfg = ConfigLoader.loadEngine(engineConfigPath);

        QueryEngine engine = switch (cfg.getType().toLowerCase()) {
            case "databricks" -> new DatabricksEngine();
            case "snowflake" -> new SnowflakeEngine();
            default -> new CustomEngine();
        };

        engine.connect(cfg);
        return engine;
    }
}
