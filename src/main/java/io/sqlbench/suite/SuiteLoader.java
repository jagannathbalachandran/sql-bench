package io.sqlbench.suite;

import io.sqlbench.config.ConfigLoader;
import io.sqlbench.config.SuiteRunConfig;
import io.sqlbench.model.QueryDefinition;

import java.io.IOException;
import java.util.List;

public class SuiteLoader {

    public static SuiteDefinition load(String suiteConfigPath, String engineName) throws IOException {
        SuiteRunConfig cfg = ConfigLoader.loadSuite(suiteConfigPath);
        List<QueryDefinition> queries = QuerySetLoader.load(cfg.getQueryDir(), engineName);
        return new SuiteDefinition(cfg, queries);
    }
}
