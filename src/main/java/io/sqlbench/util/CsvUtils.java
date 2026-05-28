package io.sqlbench.util;

import io.sqlbench.model.QueryDefinition;
import io.sqlbench.model.QueryResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvUtils {

    public static List<QueryDefinition> readQueries(Path csvPath) throws IOException {
        List<QueryDefinition> queries = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            for (CSVRecord rec : parser) {
                String alias = rec.get("query_alias").trim();
                String sql = rec.get("sql").trim();
                long expectedRows = 0;
                if (rec.isMapped("expected_row_count") && !rec.get("expected_row_count").isBlank()) {
                    try { expectedRows = Long.parseLong(rec.get("expected_row_count").trim()); }
                    catch (NumberFormatException ignored) {}
                }
                queries.add(new QueryDefinition(alias, sql, expectedRows));
            }
        }
        return queries;
    }

    public static void writeResults(Path outputPath, int runNumber, List<QueryResult> results) throws IOException {
        Files.createDirectories(outputPath.getParent());
        try (Writer writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("Run No", "Query Alias", "Query ID", "Status",
                             "Planning Time (ms)", "Execution Time (ms)",
                             "Total Client Time (ms)", "Row Count", "Error")
                     .build())) {
            for (QueryResult r : results) {
                printer.printRecord(
                        runNumber,
                        r.getQueryAlias(),
                        r.getQueryId(),
                        r.getStatus().name(),
                        r.getPlanningTimeMs() >= 0 ? r.getPlanningTimeMs() : "",
                        r.getExecutionTimeMs() >= 0 ? r.getExecutionTimeMs() : "",
                        r.getTotalClientTimeMs(),
                        r.getRowCount() >= 0 ? r.getRowCount() : "",
                        r.getErrorMessage() != null ? r.getErrorMessage() : ""
                );
            }
        }
    }
}
