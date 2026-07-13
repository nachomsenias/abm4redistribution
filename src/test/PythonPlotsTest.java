package test;

import org.junit.Test;
import util.PythonRunner;

/**
 * Manual integration checks for Python heatmap generation (requires {@code pylib} and Python).
 */
public class PythonPlotsTest {
    private static final String outFolder = ".\\experiments\\dummy\\denmark-a100-grid\\";

    @Test
    public void plotWealthTest() {
        PythonRunner.plotMap("wealth", outFolder, false);
    }

    @Test
    public void plotTauTest() {
        PythonRunner.plotMap("tau", outFolder, false);
    }

    @Test
    public void plotEffortTest() {
        PythonRunner.plotMap("effort", outFolder, false);
    }

    public static void main(String[] args) {
        PythonRunner.plotMap("wealth", outFolder, false);
        PythonRunner.plotMap("tau", outFolder, false);
        PythonRunner.plotMap("effort", outFolder, false);
    }
}
