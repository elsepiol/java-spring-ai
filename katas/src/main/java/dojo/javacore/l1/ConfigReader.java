package dojo.javacore.l1;

import dojo.Kata;
import java.nio.file.Path;

/**
 * KATA javacore L1 "ConfigReader" (tracks/01-java-core/L1-basics.md, section 8: exceptions).
 *
 * Reads a file of "key=value" lines and returns the int value of "port".
 * - Blank lines and lines starting with # are ignored; spaces around key and value are trimmed.
 * - Port must be 1..65535. If "port" appears twice, the last one wins.
 * - EVERY problem becomes a ConfigException whose message says what is wrong and which file:
 *     file missing or unreadable -> cause is the IOException (keep it! that is the real reason);
 *     no "port" line             -> message contains "port is missing";
 *     "port=abc"                 -> message contains "port" and "abc", cause is the NumberFormatException;
 *     "port=70000"               -> message contains "1..65535".
 * - The file must be closed in every case: use try-with-resources.
 */
public final class ConfigReader {

	private ConfigReader() {
	}

	public static int readPort(Path file) throws ConfigException {
		return Kata.todo("tracks/01-java-core/L1-basics.md, section 8");
	}

	/** A checked exception: the caller must decide what a broken config means (usually: refuse to start). */
	public static class ConfigException extends Exception {

		public ConfigException(String message) {
			super(message);
		}

		public ConfigException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
