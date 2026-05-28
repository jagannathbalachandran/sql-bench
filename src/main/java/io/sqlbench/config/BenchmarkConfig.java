package io.sqlbench.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BenchmarkConfig {
    private List<String> engines = new ArrayList<>();
    private String runMode = "sequential";       // sequential | concurrent | load-profile
    private int runsPerSuite = 3;
    private int queryTimeoutSeconds = 420;
    private int concurrentThreads = 10;
    private List<String> suites = new ArrayList<>();
    private OutputConfig output = new OutputConfig();

    public List<String> getEngines() { return engines; }
    public void setEngines(List<String> engines) { this.engines = engines; }
    public String getRunMode() { return runMode; }
    public void setRunMode(String runMode) { this.runMode = runMode; }
    public int getRunsPerSuite() { return runsPerSuite; }
    public void setRunsPerSuite(int runsPerSuite) { this.runsPerSuite = runsPerSuite; }
    public int getQueryTimeoutSeconds() { return queryTimeoutSeconds; }
    public void setQueryTimeoutSeconds(int v) { this.queryTimeoutSeconds = v; }
    public int getConcurrentThreads() { return concurrentThreads; }
    public void setConcurrentThreads(int concurrentThreads) { this.concurrentThreads = concurrentThreads; }
    public List<String> getSuites() { return suites; }
    public void setSuites(List<String> suites) { this.suites = suites; }
    public OutputConfig getOutput() { return output; }
    public void setOutput(OutputConfig output) { this.output = output; }
}
