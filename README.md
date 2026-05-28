# sql-bench

A generic SQL benchmarking framework for comparing any JDBC-based engine against **Databricks** and **Snowflake**. Zero vendor-specific code in the core framework — bring your own engine via a YAML config file.

## Features

- **3 run modes**: sequential, concurrent (fixed thread pool), load-profile (variable QPS over time)
- **Multi-run with best-run selection**: runs each suite N times, selects best/second/last run
- **Cross-engine comparison report**: Markdown table showing win/loss counts and latency deltas
- **Regression detection**: compares current run against a stored baseline (local or S3)
- **Row count validation**: asserts expected row counts per query
- **Query history enrichment**: back-fills server-side planning/execution times from Databricks and Snowflake APIs
- **Optional S3 upload and Slack notification**: flag-gated, off by default
- **GitHub Actions workflows**: per-engine `workflow_dispatch` triggers included

---

## Quick Start

**Requirements:** Java 17, Maven 3.8+

### 1. Add your JDBC driver

Place your custom engine's JDBC JAR in `libs/` and add it to `pom.xml` as a system-scope dependency, or install it to your local Maven repo:

```bash
mvn install:install-file \
  -Dfile=libs/my-engine-jdbc.jar \
  -DgroupId=com.mycompany \
  -DartifactId=my-engine-jdbc \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

### 2. Configure your engines

Edit the three files in `configs/engines/`:

| File | Engine |
|------|--------|
| `configs/engines/custom-engine.yml` | Your engine (JDBC driver class + URL) |
| `configs/engines/databricks.yml` | Databricks SQL warehouse |
| `configs/engines/snowflake.yml` | Snowflake virtual warehouse |

All credential fields use `${ENV_VAR}` syntax — export environment variables before running.

### 3. Add your queries

Place per-engine query CSV files in a directory under `query_files/`:

```
query_files/
└── MY_SUITE/
    ├── myengine.csv      ← engine name (lowercase, hyphenated) matches config name
    ├── databricks.csv
    └── snowflake.csv
```

CSV format:
```
query_alias,sql,expected_row_count
q1,"SELECT ...",1234
q2,"SELECT ...",0
```

### 4. Configure the benchmark

Edit `configs/benchmark.yml`:

```yaml
benchmark:
  run-mode: sequential        # sequential | concurrent | load-profile
  runs-per-suite: 3
  query-timeout-seconds: 420
  suites:
    - configs/suites/my-suite.yml
```

### 5. Build and run

```bash
./mvnw clean package -DskipTests

# Export credentials
export CUSTOM_HOST=myengine.example.com
export CUSTOM_PORT=8080
export CUSTOM_USER=user
export CUSTOM_PASSWORD=secret
export DBR_HOST=...
export DBR_TOKEN=...
# etc.

java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar
```

Results are written to `benchmark_results/`:

```
benchmark_results/
├── report.md                          ← cross-engine comparison report
├── benchmark.log
├── myengine/my-suite/run-1/results.csv
├── databricks/my-suite/run-1/results.csv
├── snowflake/my-suite/run-1/results.csv
└── baselines/                         ← regression baselines (auto-generated)
```

---

## Configuration Reference

### `configs/benchmark.yml`

| Field | Default | Description |
|-------|---------|-------------|
| `run-mode` | `sequential` | `sequential` / `concurrent` / `load-profile` |
| `runs-per-suite` | `3` | Number of full suite runs per engine |
| `query-timeout-seconds` | `420` | Per-query timeout |
| `concurrent-threads` | `10` | Threads for concurrent / load-profile mode |
| `output.local-dir` | `./benchmark_results` | Output directory |
| `output.upload-to-s3` | `false` | Upload CSVs and report to S3 |
| `output.slack-enabled` | `false` | Post summary to Slack webhook |

### Engine Types

| `type` | Class | Extra config section |
|--------|-------|----------------------|
| `custom` | `CustomEngine` | `metrics.planning-time-ms-column` |
| `databricks` | `DatabricksEngine` | `databricks.history-fetch-mode` (api/sql) |
| `snowflake` | `SnowflakeEngine` | `snowflake.fetch-query-history` |

### Validation Strategies (in suite config)

| Strategy | Behaviour |
|----------|-----------|
| `best-run` | Uses the run with lowest total success time |
| `second-run` | Always uses run #2 (cache-warmed) |
| `none` | Uses the last run |

---

## Project Structure

```
sql-bench/
├── src/main/java/io/sqlbench/
│   ├── BenchmarkMain.java          Entry point
│   ├── config/                     YAML config POJOs + loader
│   ├── engine/                     QueryEngine interface + JDBC implementations
│   ├── runner/                     Orchestrator + execution modes
│   ├── model/                      Data holders (QueryResult, BenchmarkRun, ...)
│   ├── suite/                      Suite + query-set loading
│   ├── analysis/                   Regression detection + run selection
│   ├── reporting/                  CSV writer, Markdown report, Slack
│   └── util/                       CSV, env interpolation, percentiles
├── configs/
│   ├── benchmark.yml               Master config
│   ├── engines/                    One YAML per engine
│   ├── suites/                     One YAML per query suite
│   └── load_profiles/              QPS-over-time CSVs
├── query_files/                    Per-suite, per-engine query CSVs
└── .github/workflows/              build + per-engine benchmark triggers
```

---

## GitHub Actions

Each workflow can be triggered via `workflow_dispatch` from the GitHub UI:

| Workflow | File |
|----------|------|
| Build | `.github/workflows/build.yml` |
| Custom engine benchmark | `.github/workflows/benchmark-custom.yml` |
| Databricks benchmark | `.github/workflows/benchmark-databricks.yml` |
| Snowflake benchmark | `.github/workflows/benchmark-snowflake.yml` |

Add your credentials as [GitHub Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets).

---

## Adding a New Engine

1. Create `configs/engines/my-engine.yml` with `type: custom`
2. Add per-engine query files to your suite directory (e.g. `my-engine.csv`)
3. If your engine's JDBC driver returns planning time in result metadata, set `metrics.planning-time-ms-column`
4. Add the engine path to `configs/benchmark.yml`

For non-JDBC engines, implement `QueryEngine` directly and register in `EngineFactory`.
