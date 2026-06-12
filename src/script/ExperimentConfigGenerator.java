package script;

import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility that writes a JSON template with all {@link ExperimentConfig} default values.
 * <p>
 * Usage: {@code java script.ExperimentConfigGenerator <output.json>}
 */
public class ExperimentConfigGenerator {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Output JSON path required.");
        }
        ExperimentConfig config = new ExperimentConfig();

        Gson gson = new Gson();
        String jsonText = gson.toJson(config, ExperimentConfig.class);

        try (FileWriter fw = new FileWriter(args[0])) {
            fw.write(jsonText);
        }
    }
}
