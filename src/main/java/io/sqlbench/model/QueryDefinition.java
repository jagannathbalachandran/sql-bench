package io.sqlbench.model;

public class QueryDefinition {
    private final String alias;
    private final String sql;
    private final long expectedRowCount;

    public QueryDefinition(String alias, String sql, long expectedRowCount) {
        this.alias = alias;
        this.sql = sql;
        this.expectedRowCount = expectedRowCount;
    }

    public String getAlias() { return alias; }
    public String getSql() { return sql; }
    public long getExpectedRowCount() { return expectedRowCount; }

    @Override
    public String toString() { return "QueryDefinition{alias='" + alias + "'}"; }
}
