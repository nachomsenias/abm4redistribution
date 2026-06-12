package redist;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.StatUtils;
import util.EffortCalculator;
import util.ExperimentResult;

/**
 * Agent-based model of tax voting and wealth redistribution over discrete time steps.
 * <p>
 * Each agent chooses effort to maximize utility under the current tax rate {@code tau},
 * votes on whether to decrease, keep, or increase taxes, and receives an equal share
 * of collected taxes. Initial wealth is drawn from a log-normal distribution.
 */
public class RedistributionModel {

	public static final short INITIAL_STEP = 0;

	private final int numberOfAgents;
	private final Agent[] agents;
	public enum VoteType { UP, DOWN, KEEP }

	private final int steps;

	private float tau;
	private final float[] tauValues;
	public static final float TAU_INCREMENT = 0.01f;
	public static final float MIN_TAXES = 0.0f;
	public static final float MAX_TAXES = 1;

	private final double[] populationEffortValues;
	private final double[] populationWealthValues;
	private final double[] stepGini;

	private final int[] voteDownHistory;
	private final int[] voteKeepHistory;
	private final int[] voteUpHistory;

	/** Deterministic RNG seeds indexed by Monte Carlo replication. */
	private static final int[] PRIME_SEEDS = {
		15485863, 	3311117, 	586627, 	7225709, 	1608823,
		664621, 	15527, 		20161, 		350663, 	8689,
		11689, 		14163587, 	2390911, 	33191, 		52147,
		203023, 	7253, 		10061, 		14479, 		19937,
		73553, 		24151, 		5869,  		117017, 	21277,
		113683, 	5639, 		105769,		28547,		54983,
		27407,		37589,		76367,		132527, 	164837,
		217559,		281717, 	301997,		355007, 	402137,
		7331,		9341,		40151,		78977,		4939169,
		8917871,	13963331,	5282999,	296909,		5211539,
		233, 239, 241, 251, 257, 263, 269, 271, 277, 281,
		283, 293, 307, 311, 313, 317, 331, 337, 347, 349,
		353, 359, 367, 373, 379, 383, 389, 397, 401, 409,
		419, 421, 431, 433, 439, 443, 449, 457, 461, 463,
		467, 479, 487, 491, 499, 503, 509, 521, 523, 541
	};

	public RedistributionModel(int mcIteration, int numberOfAgents, int steps, double sigma, double mu,
							   double thetaValue, int aValue, float startTau, float maxEffort) {
		this.numberOfAgents = numberOfAgents;
		this.steps = steps;

		// Gini via relative mean absolute difference (see Wikipedia links in project docs).
		stepGini = new double[steps];

		tau = startTau;
		tauValues = new float[steps];
		tauValues[INITIAL_STEP] = tau;
		populationWealthValues = new double[steps];
		populationEffortValues = new double[steps];

		MersenneTwister rng = new MersenneTwister(RedistributionModel.PRIME_SEEDS[mcIteration]);

		LogNormalDistribution distribution = new LogNormalDistribution(rng, mu, sigma);

		EffortCalculator calculator = new EffortCalculator(maxEffort);

		agents = new Agent[numberOfAgents];

		double[] initialWealth = new double[3];

		for (int i = 0; i < numberOfAgents; i++) {
			agents[i] = new Agent(thetaValue, aValue, steps, calculator);
			double wealth = distribution.sample();

			agents[i].setWealth(wealth);
			agents[i].work(startTau, INITIAL_STEP);
			updateWealth(initialWealth, agents[i].getTaxes());
		}
		// Initial redistribution at the starting tax rate.
		redistribute(agents, initialWealth[Agent.ACTUAL], INITIAL_STEP, Agent.ACTUAL);

		voteDownHistory = new int[steps];
		voteKeepHistory = new int[steps];
		voteUpHistory = new int[steps];
	}

	private void updateWealth(double[] wealth, double[] taxes) {
		for (int i = 0; i < wealth.length; i++) {
			wealth[i] += taxes[i];
		}
	}

	/** Runs all time steps from 1 to {@code steps - 1}. */
	public void run() {
		for (int t = 1; t < steps; t++) {
			double[] stepWealth = new double[3];
			for (int i = 0; i < numberOfAgents; i++) {
				agents[i].work(tau, t);
				updateWealth(stepWealth, agents[i].getTaxes());
			}

			VoteType result = Agent.doVotation(agents, stepWealth, t, voteDownHistory, voteKeepHistory, voteUpHistory);
			updateTaxes(result, t);

			switch (result) {
			case DOWN:
				redistribute(agents, stepWealth[Agent.DECREASED], t, Agent.DECREASED);
				break;
			case KEEP:
				redistribute(agents, stepWealth[Agent.ACTUAL], t, Agent.ACTUAL);
				break;
			case UP:
				redistribute(agents, stepWealth[Agent.INCREASED], t, Agent.INCREASED);
				break;
			default:
				throw new IllegalStateException("Unrecognized vote outcome.");
			}
		}

		for (int i = 0; i < numberOfAgents; i++) {
			agents[i].endStep();
		}
	}

	private void updateTaxes(VoteType result, int t) {
		switch (result) {
		case UP:
			tau = Math.min(MAX_TAXES, tau + TAU_INCREMENT);
			break;
		case DOWN:
			tau = Math.max(MIN_TAXES, tau - TAU_INCREMENT);
			break;
		case KEEP:
			break;
		default:
			break;
		}
		tauValues[t] = tau;
	}

	/**
	 * Distributes {@code recollectedWealth} equally, records per-agent income and
	 * aggregate effort, and updates the Gini coefficient for the step.
	 */
	public void redistribute(Agent[] agents, double recollectedWealth, int step, int option) {
		double redistributedWealth = recollectedWealth / agents.length;

		double[] incomeByAgent = new double[numberOfAgents];
		double stepEffort = 0;

		for (int i = 0; i < numberOfAgents; i++) {
			double income = agents[i].getSalary()[option] + redistributedWealth;
			incomeByAgent[i] = income;
			agents[i].setSalaryHistoryValue(step, income);
			stepEffort += agents[i].getEffortHistory()[step];
		}
		stepGini[step] = ExperimentResult.computeGini(incomeByAgent);

		populationWealthValues[step] = StatUtils.sum(incomeByAgent);
		populationEffortValues[step] = stepEffort;
	}

	public Agent[] getAgents() {
		return agents;
	}

	public ExperimentResult getResult() {
		ExperimentResult experimentResult = new ExperimentResult(tauValues, stepGini, populationWealthValues,
				populationEffortValues, voteDownHistory, voteKeepHistory, voteUpHistory);
		experimentResult.loadEffortHistory(this);
		experimentResult.loadSalaryHistory(this);
		experimentResult.loadVoteHistory(this);
		return experimentResult;
	}

}
