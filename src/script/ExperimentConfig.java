package script;

import lombok.Getter;

/**
 * Experiment parameters loaded from a JSON file (see {@code instances/}).
 * <p>
 * Fields omitted in the JSON keep their Java default values. Gson deserializes
 * directly into private fields; Lombok generates accessors for the rest of the code.
 */
@Getter
public class ExperimentConfig {
    /** Number of agents in each Monte Carlo replication. */
    private int numberOfAgents = 1000;
    /** Number of simulation time steps per replication. */
    private int steps = 100;
    /** Monte Carlo replications (parallel runs aggregated in output CSVs). */
    private int mc = 50;
    /** Preference for leisure vs. consumption (Cobb-Douglas exponent). */
    private float defaultTheta = 0.1f;
    /** Initial tax rate when not swept in a grid. */
    private float defaultTau = 0.1f;
    /** Effort-cost scale parameter {@code a} in the utility function. */
    private int aValue = 1;
    /** Mean of the log-normal initial wealth distribution. */
    private float mu = 0.0f;
    /** Standard deviation of the log-normal initial wealth distribution. */
    private float sigma = 0.5f;
    /** Upper bound used when searching for optimal effort. */
    private float maxEffort = 10.0f;

    // GRID sweep parameters (executionType = GRID).
    private float initialTheta = 0.01f;
    private float incTheta = 0.01f;
    private float maxTheta = 1.0f;

    private float initialTau = 0.01f;
    private float incTau = 0.01f;
    private float maxTau = 1.0f;

    private int initialA = 2;
    private int incA = 1;
    private int maxA = 100;

    /** If true, GRID mode sweeps {@code aValue}; otherwise sweeps {@code tau}. */
    private boolean useA = true;

    /** Execution mode: single run, parameter grid, or legacy PAIRS (unsupported). */
    public enum Type { SIMPLE, GRID, PAIRS }

    private Type executionType = Type.SIMPLE;

    /** Log per-replication summaries to the console. */
    private boolean printResult = false;

    /** Invoke Python line plots after a SIMPLE run. */
    private boolean runPythonScripts = false;

    /** Export step-by-step Gini coefficient CSV in GRID runs. */
    private boolean exportGini = false;
}
