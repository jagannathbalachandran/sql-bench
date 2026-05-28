package io.sqlbench.suite;

import io.sqlbench.model.QueryDefinition;
import io.sqlbench.util.CsvUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class QuerySetLoader {

    public static List<QueryDefinition> load(String queryDir, String engineName) throws IOException {
        Path dir = Path.of(queryDir);
        if (!Files.isDirectory(dir)) {
            throw new IOException("Query directory not found: " + queryDir);
        }

        // priority: <engineName>.csv → default.csv
        String normalized = engineName.toLowerCase().replaceAll("[^a-z0-9]", "-");
        Path engineCsv = dir.resolve(normalized + ".csv");
        Path defaultCsv = dir.resolve("default.csv");

        Path target;
        if (Files.exists(engineCsv)) {
            target = engineCsv;
        } else if (Files.exists(defaultCsv)) {
            target = defaultCsv;
        } else {
            // fall back to first .csv found
            target = Files.list(dir)
                    .filter(p -> p.toString().endsWith(".csv"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No CSV query file found in " + queryDir));
        }

        return CsvUtils.readQueries(target);
    }
}
