package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SuiteRunConfig {
    private String name;
    private String queryDir;
    private String validationStrategy = "best-run";  // best-run | second-run | none
    private int runsPerSuite = 3;
    private String loadProfilePath;
    private boolean rowCountValidation = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getQueryDir() { return queryDir; }
    public void setQueryDir(String queryDir) { this.queryDir = queryDir; }
    public String getValidationStrategy() { return validationStrategy; }
    public void setValidationStrategy(String v) { this.validationStrategy = v; }
    public int getRunsPerSuite() { return runsPerSuite; }
    public void setRunsPerSuite(int runsPerSuite) { this.runsPerSuite = runsPerSuite; }
    public String getLoadProfilePath() { return loadProfilePath; }
    public void setLoadProfilePath(String loadProfilePath) { this.loadProfilePath = loadProfilePath; }
    public boolean isRowCountValidation() { return rowCountValidation; }
    public void setRowCountValidation(boolean v) { this.rowCountValidation = v; }
}
