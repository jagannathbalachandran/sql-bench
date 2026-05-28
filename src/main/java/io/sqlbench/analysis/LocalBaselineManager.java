package io.sqlbench.analysis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sqlbench.model.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalBaselineManager implements BaselineManager {
    private static final Logger LOG = LoggerFactory.getLogger(LocalBaselineManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path baselineDir;

    public LocalBaselineManager(String outputDir) {
        this.baselineDir = Path.of(outputDir, "baselines");
    }

    @Override
    public Map<String, Long> loadBaseline(String suiteName) throws IOException {
        Path file = baselineFile(suiteName);
        if (!Files.exists(file)) {
            LOG.info("No baseline found for suite '{}' at {}", suiteName, file);
            return Map.of();
        }
        return MAPPER.readValue(file.toFile(), new TypeReference<>() {});
    }

    @Override
    public void saveBaseline(String suiteName, List<QueryResult> results) throws IOException {
        Files.createDirectories(baselineDir);
        Map<String, Long> baseline = new LinkedHashMap<>();
        for (QueryResult r : results) {
            if (r.isSuccess()) baseline.put(r.getQueryAlias(), r.getTotalClientTimeMs());
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(baselineFile(suiteName).toFile(), baseline);
        LOG.info("Saved baseline for suite '{}' ({} queries)", suiteName, baseline.size());
    }

    private Path baselineFile(String suiteName) {
        return baselineDir.resolve(suiteName.toLowerCase().replaceAll("[^a-z0-9]", "-") + ".json");
    }
}
