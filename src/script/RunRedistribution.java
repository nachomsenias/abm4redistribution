package script;

import com.google.gson.Gson;
import com.opencsv.CSVWriter;
import redist.RedistributionModel;
import util.ExperimentResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.FileUtils;
import util.PythonRunner;

/**
 * Main entry point for running redistribution experiments.
 * <p>
 * Reads a JSON configuration, runs one or many {@link RedistributionModel} simulations
 * (possibly in parallel), writes aggregated CSV outputs, and optionally calls Python
 * plotting scripts under {@code pylib/}.
 */
public class RunRedistribution {

	private static final Logger logger = LoggerFactory.getLogger(RunRedistribution.class);

	/**
	 * @param args {@code [outputDirectory, config.json]} — output folder and path to experiment JSON
	 */
	public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Output directory and configuration JSON path are required.");
        }
		String outFolder = args[0];
		if (System.getProperty("os.name").contains("Windows")) {
			outFolder += "\\";
		}
		ExperimentConfig config = new ExperimentConfig();
		if (args.length >= 2) {
			try {
				String file = args[1];
				logger.info("Running file: {}", file);
				config = getConfig(file);
				outFolder += FileUtils.getFileName(file) + "\\";
				File outFolderDir = new File(outFolder);
				if (!outFolderDir.exists() && !outFolderDir.mkdirs()) {
					throw new RuntimeException("Failed to create output directory tree.");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		switch (config.getExecutionType()) {
			case SIMPLE:
				simpleExperiment(config.getNumberOfAgents(), config.getSteps(), outFolder, config.getMc(),
						config.getDefaultTheta(), config.getAValue(), config.getDefaultTau(), config.getMu(),
						config.getSigma(), config.getMaxEffort(), true, true, config.isPrintResult(),
						config.isRunPythonScripts());
				break;
			case GRID:
				try {
					if (config.isUseA()) {
						runGridA(config, outFolder);
					} else {
						runGridTau(config, outFolder);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				break;
			case PAIRS:
				throw new IllegalStateException("PAIRS is no longer a supported execution mode.");
			default:
				logger.error("Execution mode not specified or unknown.");
				System.exit(1);
		}
	}

	/**
	 * Sweeps {@code theta} and {@code tau} over the ranges defined in {@link ExperimentConfig},
	 * skipping combinations that already produced output files.
	 */
	private static void runGridTau(ExperimentConfig config, String outFolder) {
		float initialTheta = config.getInitialTheta();

		while (initialTheta < config.getMaxTheta()) {
			float initialTau = config.getInitialTau();
			while (initialTau < config.getMaxTau()) {
				logger.info("### Running Theta={}, tau={}", initialTheta, initialTau);
                if (checkExistingExecution(initialTheta, initialTau, outFolder)) {
                    logger.info("### Configuration exists, skipping...");
                } else {
                    simpleExperiment(config.getNumberOfAgents(), config.getSteps(),
                            outFolder + initialTheta + "_" + initialTau + "_",
                            config.getMc(), initialTheta, config.getAValue(), initialTau, config.getMu(),
                            config.getSigma(), config.getMaxEffort(), config.isExportGini(), false,
                            config.isPrintResult(), config.isRunPythonScripts());
                }
				initialTau += config.getIncTau();
			}
			initialTheta += config.getIncTheta();
		}

		generateHeatmaps(outFolder, false);
	}

	/**
	 * Returns {@code true} if the output folder already contains CSV files for the
	 * given {@code theta} and {@code tau} prefix (resume support for long grid runs).
	 */
    private static boolean checkExistingExecution(float theta, float initialTau, String outFolder) {
        String thetaStr = String.valueOf(theta);
        String initialTauStr = String.valueOf(initialTau);

        String regexPattern = Pattern.quote(thetaStr) + "_" + Pattern.quote(initialTauStr) + "_.*";
        final Pattern pattern = Pattern.compile(regexPattern);

        File folder = new File(outFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            String errorMessage = "Output path does not exist or is not a directory: " + outFolder;
            System.err.println(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        FilenameFilter filter = (dir, name) -> pattern.matcher(name).matches();

        String[] files = folder.list(filter);
        return files != null && files.length > 0;
    }

	/**
	 * Sweeps {@code theta} and {@code aValue} over the ranges defined in {@link ExperimentConfig}.
	 */
	private static void runGridA(ExperimentConfig config, String outFolder) {
		float initialTheta = config.getInitialTheta();

		while (initialTheta < config.getMaxTheta()) {
			int initialA = config.getInitialA();
			while (initialA < config.getMaxA()) {
				logger.info("### Running Theta={}, a={}", initialTheta, initialA);
				simpleExperiment(config.getNumberOfAgents(), config.getSteps(),
						outFolder + initialTheta + "_" + initialA + "_",
						config.getMc(), initialTheta, initialA, config.getDefaultTau(), config.getMu(),
						config.getSigma(), config.getMaxEffort(), config.isExportGini(), false,
						config.isPrintResult(), config.isRunPythonScripts());
				initialA += config.getIncA();
			}
			initialTheta += config.getIncTheta();
		}

		generateHeatmaps(outFolder, true);
	}

	private static void generateHeatmaps(String outFolder, boolean useAValue) {
		logger.info("Running python scripts...");
		if (useAValue) {
			logger.info("Generating heatmaps with theta and effort cost (a)...");
		} else {
			logger.info("Generating heatmaps with theta and tau...");
		}
		PythonRunner.plotMap("wealth", outFolder, useAValue);
		PythonRunner.plotMap("tau", outFolder, useAValue);
		PythonRunner.plotMap("effort", outFolder, useAValue);
	}

	/**
	 * Runs {@code mc} independent simulations in parallel, then writes CSV files with
	 * one row per replication (Monte Carlo index) and one column per time step.
	 *
	 * @param exportGini       write {@code stepgini_*.csv}
	 * @param exportHistory    write vote and per-agent history files (SIMPLE mode)
	 * @param printResults     log each replication summary
	 * @param runPythonScripts run line plots after export
	 */
	public static void simpleExperiment(int numberOfAgents, int steps, String outFolder, int mc, double thetaValue,
										int aValue, float startTau, float mu, float sigma, float maxEffort,
										boolean exportGini, boolean exportHistory, boolean printResults,
										boolean runPythonScripts) {

		ExperimentResult[] results = new ExperimentResult[mc];

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(
				Runtime.getRuntime().availableProcessors() - 1);
		List<Future<?>> futureList = new ArrayList<>();
		List<RedistributionModel> mList = new ArrayList<>();
		for (int it = 0; it < mc; it++) {
			RedistributionModel m = new RedistributionModel(it, numberOfAgents, steps, sigma, mu, thetaValue,
					aValue, startTau, maxEffort);
			mList.add(m);
			Future<?> future = executor.submit(m::run);
			futureList.add(future);
		}
		for (int it = 0; it < mc; it++) {
			try {
				futureList.get(it).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			ExperimentResult result = mList.get(it).getResult();
			results[it] = result;
			if (printResults) {
				result.printResults(it);
			}
		}

		executor.shutdown();
		try {
			if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
				logger.warn("Executor did not terminate in time.");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String time = String.valueOf(System.currentTimeMillis());
		DecimalFormat df = new DecimalFormat("#.##");
		String tail = df.format(thetaValue) + "_" + df.format(startTau) + "_" + time + ".csv";
		String output = outFolder + "tau_" + tail;
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
			writer.writeAll(ExperimentResult.getTau(results));
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (exportGini) {
			output = outFolder + "stepgini_" + tail;
			try {
				CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
				writer.writeAll(ExperimentResult.getStepGini(results));
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		output = outFolder + "wealth_" + tail;
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
			writer.writeAll(ExperimentResult.getWealth(results));
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		output = outFolder + "effort_" + tail;
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
			writer.writeAll(ExperimentResult.getEffort(results));
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (exportHistory) {
			output = outFolder + "voteDownHistory_" + tail;
			try {
				CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
				writer.writeAll(ExperimentResult.getVoteDownHistory(results));
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			output = outFolder + "voteKeepHistory_" + tail;
			try {
				CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
				writer.writeAll(ExperimentResult.getVoteKeepHistory(results));
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			output = outFolder + "voteUpHistory_" + tail;
			try {
				CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
				writer.writeAll(ExperimentResult.getVoteUpHistory(results));
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			for (int experimentIndex = 0; experimentIndex < results.length; experimentIndex++) {
				output = outFolder + "effortHistory_" + time + "_" + experimentIndex + ".csv";
				try {
					CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
					writer.writeAll(results[experimentIndex].getEffortHistory());
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				output = outFolder + "salaryHistory_" + time + "_" + experimentIndex + ".csv";
				try {
					CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
					writer.writeAll(results[experimentIndex].getSalaryHistory());
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				output = outFolder + "voteHistory_" + time + "_" + experimentIndex + ".csv";
				try {
					CSVWriter writer = new CSVWriter(new FileWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER);
					writer.writeAll(results[experimentIndex].getVoteHistory());
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}

		if (runPythonScripts) {
			logger.info("Running python scripts...");
			PythonRunner.plotLines("tau", outFolder);
			PythonRunner.plotLines("wealth", outFolder);
			PythonRunner.plotLines("stepgini", outFolder);
		}
	}

	/** Loads experiment parameters from a JSON file. */
	public static ExperimentConfig getConfig(String jsonFile) throws IOException {
		try (Reader reader = Files.newBufferedReader(Path.of(jsonFile))) {
			return new Gson().fromJson(reader, ExperimentConfig.class);
		}
	}
}
