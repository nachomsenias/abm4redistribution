package script;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch runner for Eurozone country parameters.
 * <p>
 * Reads a CSV with columns {@code country;mu;sigma} and, for each row, runs a
 * fixed {@code theta}-{@code tau} grid via {@link RunRedistribution#simpleExperiment}.
 * <p>
 * Usage: {@code java script.RunEurozoneValues <eurozone.csv> <outputBaseDirectory>}
 */
public class RunEurozoneValues {

    private static final Logger logger = LoggerFactory.getLogger(RunEurozoneValues.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Eurozone CSV path and output base directory are required.");
        }
        int mc = 50;
        int numberOfAgents = 5000;
        int steps = 100;

        String baseOutFolder = args[1];
        if (System.getProperty("os.name").contains("Windows")) {
            baseOutFolder += "\\";
        }

        String eurozoneValuesFile = args[0];
        char fieldSeparator = ';';
        try (CSVReader csvReader = new CSVReader(new FileReader(eurozoneValuesFile), fieldSeparator,
                CSVParser.DEFAULT_QUOTE_CHARACTER, 1)) {

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                String country = line[0];
                String stringMu = line[1];
                String stringSigma = line[2];

                String outFolder = baseOutFolder + country + "\\";
                Path path = Paths.get(outFolder);
                Files.createDirectories(path);

                float mu = Float.parseFloat(stringMu);
                float sigma = Float.parseFloat(stringSigma);

                logger.info("Running grid for {}", country);
                RunEurozoneValues.runGrid(numberOfAgents, steps, mu, sigma, outFolder, mc);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sweeps normalized {@code theta} and {@code tau} on a 10..100 step grid
     * (values divided by 100) with fixed {@code a = 1}.
     */
    private static void runGrid(int numberOfAgents, int steps, float mu, float sigma, String outFolder, int mc) {
        int a = 1;
        int initialTheta = 10;
        final int incTheta = 10;
        final int maxTheta = 100;

        final int incTau = 10;
        final int maxTau = 100;

        while (initialTheta < maxTheta) {
            int initialTau = 10;
            while (initialTau < maxTau) {
                logger.info("### Running Theta={}, tau={}, a={}", initialTheta, initialTau, a);
                float thetaValue = (float) initialTheta / (float) maxTheta;
                float tauValue = (float) initialTau / (float) maxTau;
                RunRedistribution.simpleExperiment(numberOfAgents, steps,
                        outFolder + initialTheta + "_" + initialTau + "_" + a + "_",
                        mc, thetaValue, a, tauValue, mu, sigma, 10.0f, false, false, true, false);
                initialTau += incTau;
            }
            initialTheta += incTheta;
        }
    }
}
