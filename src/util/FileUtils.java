package util;

import java.io.File;

/**
 * Small helpers for experiment file paths.
 */
public class FileUtils {

	/** Returns the file name without the {@code .json} extension (used as output subfolder name). */
	public static String getFileName(String fileUrl) {
		File file = new File(fileUrl);
		String unParsedName = file.getName();
		String[] tokens = unParsedName.split(".json");
		return tokens[0];
	}
}
