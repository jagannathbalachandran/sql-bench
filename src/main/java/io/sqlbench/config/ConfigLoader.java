package io.sqlbench.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.sqlbench.util.EnvInterpolator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigLoader {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON = new ObjectMapper();

    public static BenchmarkConfig load(String configPath) throws IOException {
        String raw = Files.readString(Path.of(configPath));
        String interpolated = EnvInterpolator.interpolate(raw);
        JsonNode root = YAML.readTree(interpolated);
        JsonNode benchmarkNode = root.path("benchmark");
        return YAML.treeToValue(benchmarkNode, BenchmarkConfig.class);
    }

    public static EngineConfig loadEngine(String engineConfigPath) throws IOException {
        String raw = Files.readString(Path.of(engineConfigPath));
        String interpolated = EnvInterpolator.interpolate(raw);
        JsonNode root = YAML.readTree(interpolated);
        JsonNode engineNode = root.path("engine");
        String type = engineNode.path("type").asText("custom");

        return switch (type.toLowerCase()) {
            case "databricks" -> YAML.treeToValue(engineNode, DatabricksEngineConfig.class);
            case "snowflake" -> YAML.treeToValue(engineNode, SnowflakeEngineConfig.class);
            default -> YAML.treeToValue(engineNode, CustomEngineConfig.class);
        };
    }

    public static SuiteRunConfig loadSuite(String suiteConfigPath) throws IOException {
        String raw = Files.readString(Path.of(suiteConfigPath));
        String interpolated = EnvInterpolator.interpolate(raw);
        JsonNode root = YAML.readTree(interpolated);
        JsonNode suiteNode = root.path("suite");
        return YAML.treeToValue(suiteNode, SuiteRunConfig.class);
    }
}
