package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Launches Python plotting scripts from {@code ../pylib/src/} as subprocesses.
 */
public class PythonRunner {

    private static final Logger logger = LoggerFactory.getLogger(PythonRunner.class);

    /** Line plots for a given metric prefix (e.g. {@code tau}, {@code wealth}). */
    public static void plotLines(String data, String folder) {
        String workingDir = "./pylib/src/";
        ProcessBuilder processBuilder = new ProcessBuilder("python", ".\\pylib\\src\\plot-lines-2.py", data, folder);
        processBuilder.directory(new File(workingDir));
        runScript(processBuilder);
    }

    /**
     * Heatmaps over grid results.
     *
     * @param useAValue {@code true} for theta–a grids, {@code false} for theta–tau grids
     */
    public static void plotMap(String data, String folder, boolean useAValue) {
        String flag = String.valueOf(useAValue);
        String capitalFlag = flag.substring(0, 1).toUpperCase() + flag.substring(1);
        String workingDir = "./pylib/src/";
        ProcessBuilder processBuilder = new ProcessBuilder("python", "scripts\\plot-heatmap.py", data, folder, capitalFlag);
        processBuilder.directory(new File(workingDir));
        processBuilder.environment().put("PYTHONPATH", workingDir);
        runScript(processBuilder);
    }

    private static void runScript(ProcessBuilder processBuilder) {
        try {
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info(line);
            }

            int exitCode = process.waitFor();
            logger.info("Python script finished with exit code: {}", exitCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
