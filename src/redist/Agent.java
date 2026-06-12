package redist;

import redist.RedistributionModel.VoteType;
import util.EffortCalculator;

/**
 * Economic agent with Cobb-Douglas preferences, effort choice, and majority-style tax voting.
 * <p>
 * Salary and tax contributions are precomputed for three scenarios: tax decrease, unchanged,
 * and tax increase ({@link #DECREASED}, {@link #ACTUAL}, {@link #INCREASED}).
 */
public class Agent {
	private double wealth;
	public static final int DECREASED = 0;
	public static final int ACTUAL = 1;
	public static final int INCREASED = 2;

	private final double[] salary;
	private final double[] taxes;

	private final VoteType[] voteHistory;
	private boolean changedItsVoteDuringTheSimulation;

	private final double theta;
	private final int a;

	private final double[] effortHistory;
	private final double[] salaryHistory;

	private final EffortCalculator calculator;

	public Agent(double thetaValue, int a, int numSteps, EffortCalculator calculator) {
		this.theta = thetaValue;
		this.a = a;

		salary = new double[3];
		taxes = new double[3];

		effortHistory = new double[numSteps];
		salaryHistory = new double[numSteps];
		voteHistory = new VoteType[numSteps];

		this.calculator = calculator;
	}

	public void setWealth(double income) {
		this.wealth = income;
	}

	public double[] getSalary() {
		return salary;
	}

	/**
	 * Chooses effort for the current step and fills salary/tax arrays for all three tax scenarios.
	 */
	public void work(float tau, int t) {
		double effort = calculator.getEffort(theta, a, wealth, tau);

		effortHistory[t] = effort;

		float tauDecreased = Math.max(RedistributionModel.MIN_TAXES, tau - RedistributionModel.TAU_INCREMENT);
		double[] decreased = calculator.utilityAndTaxes(theta, a, wealth, tauDecreased, effort);
		salary[DECREASED] = decreased[0];
		taxes[DECREASED] = decreased[1];

		double[] actual = calculator.utilityAndTaxes(theta, a, wealth, tau, effort);
		salary[ACTUAL] = actual[0];
		taxes[ACTUAL] = actual[1];

		float tauIncreased = Math.min(RedistributionModel.MAX_TAXES, tau + RedistributionModel.TAU_INCREMENT);
		double[] increased = calculator.utilityAndTaxes(theta, a, wealth, tauIncreased, effort);
		salary[INCREASED] = increased[0];
		taxes[INCREASED] = increased[1];
	}

	public double[] getTaxes() {
		return taxes;
	}

	/**
	 * Votes for the tax scenario that maximizes post-redistribution income
	 * (equal per-capita share of collected taxes plus own salary).
	 */
	public VoteType vote(double[] share, int step) {

		double max = share[ACTUAL] + salary[ACTUAL];
		int option = ACTUAL;

		if (share[DECREASED] + salary[DECREASED] > max) {
			max = share[DECREASED] + salary[DECREASED];
			option = DECREASED;
		}
		if (share[INCREASED] + salary[INCREASED] > max) {
			option = INCREASED;
		}

		VoteType vote;
		switch (option) {
			case DECREASED:
				vote = VoteType.DOWN;
				break;
			case ACTUAL:
				vote = VoteType.KEEP;
				break;
			case INCREASED:
				vote = VoteType.UP;
				break;
			default:
				throw new IllegalStateException("Illegal option value.");
		}
		voteHistory[step] = vote;
		return vote;
	}

	/**
	 * Aggregates individual votes and returns the plurality outcome (DOWN, KEEP, or UP).
	 */
	public static VoteType doVotation(Agent[] agents, double[] wealth, int step, int[] voteDownHistory,
									  int[] voteKeepHistory, int[] voteUpHistory) {

		int voteUps = 0;
		int voteDowns = 0;
		int voteKeeps = 0;

		double[] share = new double[3];
		share[DECREASED] = wealth[DECREASED] / agents.length;
		share[ACTUAL] = wealth[ACTUAL] / agents.length;
		share[INCREASED] = wealth[INCREASED] / agents.length;

		for (Agent agent : agents) {
			VoteType vote = agent.vote(share, step);
			switch (vote) {
				case UP:
					voteUps++;
					break;
				case DOWN:
					voteDowns++;
					break;
				case KEEP:
					voteKeeps++;
					break;
				default:
					break;
			}
		}

		voteDownHistory[step] = voteDowns;
		voteKeepHistory[step] = voteKeeps;
		voteUpHistory[step] = voteUps;

		if (voteDowns > voteKeeps && voteDowns > voteUps) {
			return VoteType.DOWN;
		} else if (voteUps > voteKeeps && voteUps > voteDowns) {
			return VoteType.UP;
		} else {
			return VoteType.KEEP;
		}
	}

	public double[] getEffortHistory() {
		return effortHistory;
	}

	public double[] getSalaryHistory() {
		return salaryHistory;
	}

	public void setSalaryHistoryValue(int step, double salaryValue) {
		this.salaryHistory[step] = salaryValue;
	}

	public VoteType[] getVoteHistory() {
		return voteHistory;
	}

	/** Marks whether the agent ever changed its vote after the first voting step. */
	public void endStep() {
		VoteType firstVote = voteHistory[1];
		int step = 2;
		while (step < voteHistory.length && !changedItsVoteDuringTheSimulation) {
			if (voteHistory[step] != firstVote) {
				changedItsVoteDuringTheSimulation = true;
			}
			step++;
		}
	}
}
