# Redistribution model

Agent-based simulation where heterogeneous agents choose labor effort, vote on the tax rate, and receive equal per-capita redistribution of tax revenue. The project runs many Monte Carlo simulations in parallel, exports results as semicolon-separated CSV files, and can generate plots via optional Python scripts.

## Requirements

- Java 11+ (Maven `release` is 11; newer JDKs are supported for compilation)
- Maven 3.x
- Python 3 (optional, for plots under `pylib/`)

## Build

```bash
mvn compile
```

## Main entry point: `RunRedistribution`

Class: `script.RunRedistribution`

### Command line

```text
java script.RunRedistribution <outputDirectory> <config.json>
```

| Argument | Description |
|----------|-------------|
| `outputDirectory` | Base folder for results. On Windows a trailing `\` is appended automatically. |
| `config.json` | Path to an experiment definition (see `instances/`). |

The program creates a subfolder named after the config file (without `.json`), e.g. `experiments\sigma05-grid\` when using `instances/sigma05-grid.json`.

### Execution flow

1. **Load configuration** — JSON is parsed into `ExperimentConfig` (Gson). Missing keys keep Java default values.
2. **Choose mode** — `executionType` in the JSON:
   - **`SIMPLE`** — one parameter set, full history export, optional line plots.
   - **`GRID`** — nested loops over parameter ranges; heatmaps at the end.
   - **`PAIRS`** — removed; throws an error if selected.
3. **Run simulations** — `simpleExperiment` launches `mc` independent `RedistributionModel` instances on a thread pool (`availableProcessors - 1` threads).
4. **Write CSVs** — aggregated matrices (one row per Monte Carlo replication, one column per time step).
5. **Optional Python** — line plots (SIMPLE) or heatmaps (GRID) via `PythonRunner`.

### Configuration file (`ExperimentConfig`)

Example minimal SIMPLE run (`instances/default-simple.json`):

```json
{
  "numberOfAgents": 1000,
  "steps": 100,
  "mc": 50,
  "defaultTheta": 0.1,
  "defaultTau": 0.1,
  "aValue": 10,
  "mu": 0.0,
  "sigma": 0.5,
  "executionType": "SIMPLE"
}
```

| Field | Role |
|-------|------|
| `numberOfAgents` | Population size per replication |
| `steps` | Number of time steps |
| `mc` | Monte Carlo replications |
| `defaultTheta` | Preference parameter θ (leisure vs. consumption) |
| `defaultTau` | Initial / fixed tax rate τ |
| `aValue` | Effort-cost scale *a* |
| `mu`, `sigma` | Log-normal parameters for initial wealth |
| `maxEffort` | Upper bound when optimizing effort (default 10) |
| `executionType` | `SIMPLE`, `GRID`, or `PAIRS` |
| `printResult` | Log each replication summary |
| `runPythonScripts` | After SIMPLE: run line plots |
| `exportGini` | In GRID: write `stepgini_*.csv` per point |

**GRID mode** (`executionType`: `"GRID"`) additionally uses:

| Field | Role |
|-------|------|
| `initialTheta`, `incTheta`, `maxTheta` | Sweep range for θ |
| `initialTau`, `incTau`, `maxTau` | Sweep range for τ (when `useA` is false) |
| `initialA`, `incA`, `maxA` | Sweep range for *a* (when `useA` is true) |
| `useA` | `true` → grid over θ and *a*; `false` → grid over θ and τ |

For θ–τ grids, existing output files matching `{theta}_{tau}_*` are skipped so long runs can be resumed.

Generate a template JSON with defaults:

```bash
mvn -q exec:java -Dexec.mainClass="script.ExperimentConfigGenerator" -Dexec.args="instances/my-config.json"
```

(Or run the class from your IDE with the output path as the sole argument.)

### Output files

Each call to `simpleExperiment` writes files under the experiment folder with a suffix `{theta}_{tau}_{timestamp}.csv` (formatted to two decimals).

| Prefix | Content |
|--------|---------|
| `tau_` | Tax rate τ over time (one row per MC replication) |
| `wealth_` | Total population wealth per step |
| `effort_` | Total population effort per step |
| `stepgini_` | Gini coefficient per step (if `exportGini` or SIMPLE defaults) |
| `voteDownHistory_`, `voteKeepHistory_`, `voteUpHistory_` | Vote counts per step (SIMPLE with history) |
| `effortHistory_*`, `salaryHistory_*`, `voteHistory_*` | Per-agent series (SIMPLE only) |

CSV format: semicolon separator, no quote character.

After a **GRID** run, `PythonRunner` generates heatmaps for `wealth`, `tau`, and `effort` from the collected CSVs.

After **SIMPLE** with `runPythonScripts: true`, line plots are produced for `tau`, `wealth`, and `stepgini`.

## Simulation model (summary)

- **`RedistributionModel`** — time loop: agents work, aggregate taxes under three τ scenarios, vote, update τ, redistribute equally, record Gini and aggregates.
- **`Agent`** — optimizes effort via `EffortCalculator`, votes for the scenario that maximizes post-redistribution income.
- **`EffortCalculator`** — Cobb–Douglas utility with tax and quadratic effort cost; Nelder–Mead optimization with grid fallback.

RNG seed per replication: `PRIME_SEEDS[mcIteration]` in `RedistributionModel`.

## Other utilities

| Class | Purpose |
|-------|---------|
| `ExperimentConfigGenerator` | Writes default JSON to a path |
| `RunEurozoneValues` | Reads `country;mu;sigma` CSV and runs a fixed grid per country |
| `PythonPlotsTest` | JUnit hooks to test Python heatmaps locally |

## Project layout

```text
src/
  script/     RunRedistribution, ExperimentConfig, batch runners
  redist/     RedistributionModel, Agent
  util/       ExperimentResult, EffortCalculator, FileUtils, PythonRunner
  test/       Python integration tests
instances/    Sample JSON configurations
experiments/  Typical output location (created at runtime)
pylib/        Python plotting scripts (external to Maven)
```

## Example

```bash
mvn -q compile
java -cp target/classes:... script.RunRedistribution experiments instances/default-simple.json
```

Adjust the classpath to include Maven dependencies (Gson, OpenCSV, Commons Math, SLF4J, etc.) or configure an exec plugin / IDE run configuration pointing at `script.RunRedistribution` with the two arguments above.
