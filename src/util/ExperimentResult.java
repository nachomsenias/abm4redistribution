package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redist.Agent;
import redist.RedistributionModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Aggregated time series from one {@link RedistributionModel} run, formatted for CSV export.
 * <p>
 * Rows correspond to Monte Carlo replications when merged in {@link script.RunRedistribution};
 * columns correspond to simulation time steps.
 */
public class ExperimentResult {

	private final float[] tauValues;
	private final double[] stepGini;
	private final double[] wealth;
	private final double[] effort;

	private final int[] voteDownHistory;
	private final int[] voteKeepHistory;
	private final int[] voteUpHistory;

	private double[][] effortHistoryByAgent;
	private double[][] salaryHistoryByAgent;
	private RedistributionModel.VoteType[][] voteHistoryByAgent;

	private static final Logger logger = LoggerFactory.getLogger(ExperimentResult.class);

	public ExperimentResult(float[] tauValues, double[] stepGini, double[] wealth, double[] effort,
							int[] voteDownHistory, int[] voteKeepHistory, int[] voteUpHistory) {
		this.tauValues = tauValues;
		this.stepGini = stepGini;
		this.wealth = wealth;
		this.effort = effort;
		this.voteDownHistory = voteDownHistory;
		this.voteKeepHistory = voteKeepHistory;
		this.voteUpHistory = voteUpHistory;
	}

	public void loadEffortHistory(RedistributionModel model) {
		Agent[] agents = model.getAgents();
		this.effortHistoryByAgent = new double[agents.length][];
		for (int agentIndex = 0; agentIndex < effortHistoryByAgent.length; agentIndex++) {
			effortHistoryByAgent[agentIndex] = agents[agentIndex].getEffortHistory();
		}
	}

	public void loadSalaryHistory(RedistributionModel model) {
		Agent[] agents = model.getAgents();
		this.salaryHistoryByAgent = new double[agents.length][];
		for (int agentIndex = 0; agentIndex < salaryHistoryByAgent.length; agentIndex++) {
			salaryHistoryByAgent[agentIndex] = agents[agentIndex].getSalaryHistory();
		}
	}

	public void loadVoteHistory(RedistributionModel model) {
		Agent[] agents = model.getAgents();
		this.voteHistoryByAgent = new RedistributionModel.VoteType[agents.length][];
		for (int agentIndex = 0; agentIndex < this.voteHistoryByAgent.length; agentIndex++) {
			this.voteHistoryByAgent[agentIndex] = agents[agentIndex].getVoteHistory();
		}
	}

	public void printResults(int iteration) {
		logger.info("########################## Finished iteration: {} ##########################", iteration);
		logger.info("Tau values: {}", Arrays.toString(tauValues));
		logger.info("Step Gini: {}", Arrays.toString(stepGini));
		logger.info("Wealth: {}", Arrays.toString(wealth));
	}

	private List<String[]> getStringValues(double[][] doubleValues) {
		List<String[]> values = new ArrayList<>();
		for (double[] doubles : doubleValues) {
			String[] strValues = new String[doubles.length];

			for (int t = 0; t < doubles.length; t++) {
				strValues[t] = String.valueOf(doubles[t]).replace('.', ',');
			}
			values.add(strValues);
		}
		return values;
	}

	private List<String[]> getVoteTypeStringValues(RedistributionModel.VoteType[][] voteValues) {
		List<String[]> values = new ArrayList<>();
		for (RedistributionModel.VoteType[] votes : voteValues) {
			String[] strValues = new String[votes.length];

			for (int t = 0; t < votes.length; t++) {
				RedistributionModel.VoteType vote = votes[t];
				if (vote == RedistributionModel.VoteType.DOWN) {
					strValues[t] = String.valueOf(-1);
				} else if (vote == RedistributionModel.VoteType.UP) {
					strValues[t] = String.valueOf(1);
				} else {
					strValues[t] = String.valueOf(0);
				}
			}
			values.add(strValues);
		}
		return values;
	}

	public List<String[]> getEffortHistory() {
		return this.getStringValues(this.effortHistoryByAgent);
	}

	public List<String[]> getSalaryHistory() {
		return this.getStringValues(this.salaryHistoryByAgent);
	}

	public List<String[]> getVoteHistory() {
		return this.getVoteTypeStringValues(this.voteHistoryByAgent);
	}

	/**
	 * Gini coefficient via the relative mean absolute difference formulation.
	 */
	public static double computeGini(double[] populationValues) {
		double md = 0;
		double am = 0;
		int n = populationValues.length;

		for (int i = 0; i < n; i++) {
			am += populationValues[i];
			for (int j = 0; j < n; j++) {
				if (i == j) {
					continue;
				}
				md += Math.abs(populationValues[i] - populationValues[j]);
			}
		}

		return (md / (am * n - 1)) / 2;
	}

	public static List<String[]> getTau(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.tauValues.length];

			for (int t = 0; t < result.tauValues.length; t++) {
				strValues[t] = String.valueOf(result.tauValues[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getStepGini(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.stepGini.length];

			for (int t = 0; t < result.stepGini.length; t++) {
				strValues[t] = String.valueOf(result.stepGini[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getWealth(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.wealth.length];

			for (int t = 0; t < result.wealth.length; t++) {
				strValues[t] = String.valueOf(result.wealth[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getEffort(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.effort.length];

			for (int t = 0; t < result.effort.length; t++) {
				strValues[t] = String.valueOf(result.effort[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getVoteDownHistory(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.voteDownHistory.length];

			for (int t = 0; t < result.voteDownHistory.length; t++) {
				strValues[t] = String.valueOf(result.voteDownHistory[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getVoteKeepHistory(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.voteKeepHistory.length];

			for (int t = 0; t < result.voteKeepHistory.length; t++) {
				strValues[t] = String.valueOf(result.voteKeepHistory[t]);
			}
			values.add(strValues);
		}
		return values;
	}

	public static List<String[]> getVoteUpHistory(ExperimentResult[] results) {
		List<String[]> values = new ArrayList<>();
		for (ExperimentResult result : results) {
			String[] strValues = new String[result.voteUpHistory.length];

			for (int t = 0; t < result.voteUpHistory.length; t++) {
				strValues[t] = String.valueOf(result.voteUpHistory[t]);
			}
			values.add(strValues);
		}
		return values;
	}
}
