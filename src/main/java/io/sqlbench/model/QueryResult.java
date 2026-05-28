package io.sqlbench.model;

public class QueryResult {
    public enum Status { SUCCESS, FAILED, TIMED_OUT }

    private final String engineName;
    private final String queryAlias;
    private String queryId = "";
    private final Status status;
    private long planningTimeMs = -1;
    private long executionTimeMs = -1;
    private final long totalClientTimeMs;
    private long rowCount = -1;
    private final String errorMessage;

    private QueryResult(String engineName, String queryAlias, Status status,
                        long totalClientTimeMs, String errorMessage) {
        this.engineName = engineName;
        this.queryAlias = queryAlias;
        this.status = status;
        this.totalClientTimeMs = totalClientTimeMs;
        this.errorMessage = errorMessage;
    }

    public static QueryResult success(String engine, String alias, long clientMs, long rows) {
        QueryResult r = new QueryResult(engine, alias, Status.SUCCESS, clientMs, null);
        r.rowCount = rows;
        return r;
    }

    public static QueryResult failed(String engine, String alias, long clientMs, String error) {
        return new QueryResult(engine, alias, Status.FAILED, clientMs, error);
    }

    public static QueryResult timedOut(String engine, String alias, long clientMs) {
        return new QueryResult(engine, alias, Status.TIMED_OUT, clientMs, "Query timed out");
    }

    public String getEngineName() { return engineName; }
    public String getQueryAlias() { return queryAlias; }
    public String getQueryId() { return queryId; }
    public void setQueryId(String queryId) { this.queryId = queryId; }
    public Status getStatus() { return status; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public long getPlanningTimeMs() { return planningTimeMs; }
    public void setPlanningTimeMs(long planningTimeMs) { this.planningTimeMs = planningTimeMs; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public long getTotalClientTimeMs() { return totalClientTimeMs; }
    public long getRowCount() { return rowCount; }
    public void setRowCount(long rowCount) { this.rowCount = rowCount; }
    public String getErrorMessage() { return errorMessage; }
}
