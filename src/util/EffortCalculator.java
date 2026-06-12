package util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Utility maximization and optimal effort for agents with Cobb-Douglas preferences
 * and quadratic effort cost {@code effort² / (2a)}.
 */
public class EffortCalculator {

	private final float maxEffort;

	public EffortCalculator(float maxEffort) {
		this.maxEffort = maxEffort;
	}

	private static final class FitnessCalculator implements MultivariateFunction {

		private final double theta;
		private final int a;
		private final double wealth;
		private final float tau;
		private final EffortCalculator calculator;

		public FitnessCalculator(double thetaValue, int a, double wealth, float tau, EffortCalculator calculator) {
			this.a = a;
			this.theta = thetaValue;
			this.wealth = wealth;
			this.tau = tau;
			this.calculator = calculator;
		}

		@Override
		public double value(double[] point) {
			return calculator.utilityFunction(theta, a, wealth, tau, point[0]);
		}
	}

	private double utility(double theta, double wealth, double effort) {
		double wealthValues = Math.pow(wealth, (1 - theta));
		double workPayoff = Math.pow(effort, theta);
		return (wealthValues * workPayoff);
	}

	private double effortCost(int a, double effort) {
		return (Math.pow(effort, 2) / (2 * a));
	}

	public double utilityFunction(double theta, int a, double wealth, float tau, double effort) {
		double utility = utility(theta, wealth, effort) * (1.0f - tau);
		double effortCost = effortCost(a, effort);
		return utility - effortCost;
	}

	/** @return {@code [utility, taxes]} for the given effort and tax rate */
	public double[] utilityAndTaxes(double theta, int a, double wealth, float tau, double effort) {
		double[] values = new double[2];
		values[0] = utilityFunction(theta, a, wealth, tau, effort);
		values[1] = (values[0] * tau) / (1.0f - tau);
		return values;
	}

	/**
	 * Maximizes {@link #utilityFunction} over effort using Nelder-Mead;
	 * falls back to a grid search if the optimizer exceeds its evaluation budget.
	 */
	public double getEffort(double theta, int a, double wealth, float tau) {
		NelderMeadSimplex simplex = new NelderMeadSimplex(1);
		FitnessCalculator calc = new FitnessCalculator(theta, a, wealth, tau, this);

		SimplexOptimizer optimizer = new SimplexOptimizer(1e-10, 1e-30);
		int maxEvals = 1000;
		double[] guess = {1.0};
		double maxValue;
		try {
			final PointValuePair best = optimizer.optimize(
					new MaxEval(maxEvals),
					new ObjectiveFunction(calc),
					simplex,
					GoalType.MAXIMIZE,
					new InitialGuess(guess)
			);
			maxValue = best.getPoint()[0];
		} catch (TooManyEvaluationsException exception) {
			float eValue = 0.0f;
			float granularity = 0.01f;

			int numValues = (int) ((1.0f / granularity) * maxEffort);

			double[] utilityValueList = new double[numValues];
			double[] effortValueList = new double[numValues];
			int index = 0;

			while (eValue < maxEffort && index < numValues) {
				utilityValueList[index] = utilityFunction(theta, a, wealth, tau, eValue);
				effortValueList[index] = eValue;
				eValue += granularity;
				index++;
			}
			double max = StatUtils.max(utilityValueList);
			int maxIndex = ArrayUtils.indexOf(utilityValueList, max);
			maxValue = effortValueList[maxIndex];
		}
		return maxValue;
	}
}
