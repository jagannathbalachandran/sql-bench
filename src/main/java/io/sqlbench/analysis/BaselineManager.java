package io.sqlbench.analysis;

import io.sqlbench.model.QueryResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BaselineManager {
    Map<String, Long> loadBaseline(String suiteName) throws IOException;
    void saveBaseline(String suiteName, List<QueryResult> results) throws IOException;
}
