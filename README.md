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

## Running the Benchmark

### Full command-line syntax

```bash
java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar [config-path]
```

`config-path` defaults to `configs/benchmark.yml` if omitted.

```bash
# Default config
java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar

# Explicit config path
java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar configs/benchmark.yml

# Override config path with a custom file
java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar configs/my-custom-run.yml
```

---

### Run against a single engine

Remove all other engines from `configs/benchmark.yml`, leaving only the one you want:

```yaml
# configs/benchmark.yml
benchmark:
  engines:
    - configs/engines/snowflake.yml   # only Snowflake
```

No code changes or rebuilds are needed — the file is read at runtime.

To switch to Databricks only:

```yaml
  engines:
    - configs/engines/databricks.yml
```

To run all engines side-by-side (comparison mode):

```yaml
  engines:
    - configs/engines/snowflake.yml
    - configs/engines/databricks.yml
    - configs/engines/custom-engine.yml
```

The `ComparisonReporter` will automatically produce a side-by-side latency table in `benchmark_results/report.md` when more than one engine is listed.

---

### Run against a specific suite

Change the `suites` list in `configs/benchmark.yml`:

```yaml
  suites:
    - configs/suites/tpcds-100gb.yml   # 100 GB TPC-DS on Snowflake sample data
```

To run multiple suites in one go:

```yaml
  suites:
    - configs/suites/tpcds-100gb.yml
    - configs/suites/tpcds-1tb.yml
```

Each engine × suite combination runs independently and gets its own results directory.

---

### Change the number of runs per suite

Edit `runs-per-suite` in the **suite config file** (e.g. `configs/suites/tpcds-100gb.yml`):

```yaml
suite:
  name: TPCDS_100GB
  query-dir: query_files/TPCDS_100GB
  runs-per-suite: 3       # ← change this (1 = single pass, 3 = warm-cache best-of-3)
  validation: best-run    # best-run | second-run | none
  row-count-validation: false
```

| `runs-per-suite` | When to use |
|-----------------|-------------|
| `1` | Quick smoke-test / CI check |
| `2` | Run once cold, once warm — use `validation: second-run` |
| `3` | Standard benchmark — use `validation: best-run` to pick the fastest |

> **Note:** The field must be spelled `runs-per-suite` (kebab-case). A field named just `runs` is not recognised and will silently use the default of `3`.

---

### Environment variables

All credential fields in engine YAML files use `${VAR_NAME}` placeholders. Set the corresponding environment variables before running.

#### Snowflake

```bash
export SF_ACCOUNT="orgname-accountname"      # e.g. vizxhnu-pg58840 (no .snowflakecomputing.com)
export SF_USER="your_username"
export SF_PASSWORD="your_password"
export SF_ROLE="ACCOUNTADMIN"                # role with USAGE on warehouse + database
export SF_WAREHOUSE="COMPUTE_WH"             # virtual warehouse to benchmark
export SF_DATABASE="SNOWFLAKE_SAMPLE_DATA"   # or your own database
export SF_SCHEMA="TPCDS_SF10TCL"             # 100 GB TPC-DS; use TPCDS_SF100TCL for 1 TB
export SF_REPORTING_WAREHOUSE="COMPUTE_WH"   # warehouse used to read QUERY_HISTORY (optional)
```

| Scale | `SF_SCHEMA` value | Approx size |
|-------|-------------------|-------------|
| 100 GB | `TPCDS_SF10TCL` | SF10 scale factor |
| 1 TB | `TPCDS_SF100TCL` | SF100 scale factor |

> **Java 17 note:** The Snowflake JDBC driver uses Apache Arrow for result transfer, which requires `--add-opens` flags on Java 17+. The engine config avoids this by setting `JDBC_QUERY_RESULT_FORMAT: JSON` under `properties`, which switches to JSON-based transfer with no JVM flag changes needed.

#### Databricks

```bash
export DBR_HOST="your-workspace.azuredatabricks.net"   # workspace hostname, no https://
export DBR_HTTP_PATH="/sql/1.0/warehouses/abc123"      # SQL warehouse HTTP path
export DBR_TOKEN="dapiXXXXXXXXXXXXXXXX"               # personal access token
```

#### AWS S3 (optional — only needed when `upload-to-s3: true`)

```bash
export S3_BUCKET="my-benchmark-results"
export AWS_ACCESS_KEY_ID="..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_REGION="us-east-1"
```

#### Slack (optional — only needed when `slack-enabled: true`)

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
```

---

### Complete example: Snowflake TPC-DS 100 GB, single run

```bash
# 1. Build (only needed once, or after code changes)
./mvnw package -DskipTests

# 2. Set credentials
export SF_ACCOUNT="<orgname-accountname>"
export SF_USER="<your_username>"
export SF_PASSWORD="<your_password>"
export SF_ROLE="ACCOUNTADMIN"
export SF_WAREHOUSE="<your_warehouse>"
export SF_DATABASE="SNOWFLAKE_SAMPLE_DATA"
export SF_SCHEMA="TPCDS_SF10TCL"

# 3. Run
java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar configs/benchmark.yml
```

Results land in `./benchmark_results/`:

```
benchmark_results/
├── report.md                                   ← summary + regression table
├── snowflake/tpcds-100gb/run-1/results.csv     ← per-query timings
└── baselines/tpcds-100gb-snowflake.json        ← saved baseline for next run
```

---

### Comparing warehouse sizes (e.g. MEDIUM vs LARGE)

1. Duplicate the engine config:
   ```bash
   cp configs/engines/snowflake.yml configs/engines/snowflake-large.yml
   ```

2. In `snowflake-large.yml` change the warehouse placeholder:
   ```yaml
   warehouse: "${SF_WAREHOUSE_LARGE}"
   ```

3. Add both to `benchmark.yml`:
   ```yaml
   engines:
     - configs/engines/snowflake.yml
     - configs/engines/snowflake-large.yml
   ```

4. Export both variables and run:
   ```bash
   export SF_WAREHOUSE="COMPUTE_WH"
   export SF_WAREHOUSE_LARGE="LARGE_WH"
   java -jar target/sql-bench-1.0.0-jar-with-dependencies.jar
   ```

The Markdown report will show a side-by-side latency comparison between the two.

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
