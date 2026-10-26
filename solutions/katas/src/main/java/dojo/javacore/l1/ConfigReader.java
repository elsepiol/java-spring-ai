package dojo.javacore.l1;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reference solution. */
public final class ConfigReader {

	private ConfigReader() {
	}

	public static int readPort(Path file) throws ConfigException {
		String raw = null;
		// try-with-resources: reader.close() runs however the block ends (return, exception).
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.strip();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int eq = trimmed.indexOf('=');
				if (eq > 0 && trimmed.substring(0, eq).strip().equals("port")) {
					raw = trimmed.substring(eq + 1).strip();
				}
			}
		} catch (IOException e) {
			throw new ConfigException("cannot read config file " + file, e); // wrap, keep the cause
		}
		if (raw == null) {
			throw new ConfigException("port is missing in " + file);
		}
		int port;
		try {
			port = Integer.parseInt(raw);
		} catch (NumberFormatException e) {
			throw new ConfigException("port '" + raw + "' in " + file + " is not a number", e);
		}
		if (port < 1 || port > 65535) {
			throw new ConfigException("port " + port + " in " + file + " must be in 1..65535");
		}
		return port;
	}

	public static class ConfigException extends Exception {

		public ConfigException(String message) {
			super(message);
		}

		public ConfigException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
