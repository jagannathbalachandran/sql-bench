package io.sqlbench.suite;

import io.sqlbench.config.SuiteRunConfig;
import io.sqlbench.model.QueryDefinition;

import java.util.List;

public class SuiteDefinition {
    private final String name;
    private final String queryDir;
    private final String validationStrategy;
    private final int runsPerSuite;
    private final String loadProfilePath;
    private final boolean rowCountValidation;
    private final List<QueryDefinition> queries;

    public SuiteDefinition(SuiteRunConfig cfg, List<QueryDefinition> queries) {
        this.name = cfg.getName();
        this.queryDir = cfg.getQueryDir();
        this.validationStrategy = cfg.getValidationStrategy();
        this.runsPerSuite = cfg.getRunsPerSuite();
        this.loadProfilePath = cfg.getLoadProfilePath();
        this.rowCountValidation = cfg.isRowCountValidation();
        this.queries = queries;
    }

    public String getName() { return name; }
    public String getQueryDir() { return queryDir; }
    public String getValidationStrategy() { return validationStrategy; }
    public int getRunsPerSuite() { return runsPerSuite; }
    public String getLoadProfilePath() { return loadProfilePath; }
    public boolean isRowCountValidation() { return rowCountValidation; }
    public List<QueryDefinition> getQueries() { return queries; }
}
