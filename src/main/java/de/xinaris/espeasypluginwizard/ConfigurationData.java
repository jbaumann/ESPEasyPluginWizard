package de.xinaris.espeasypluginwizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * This class holds all the configuration data needed for the main app.
 *
 * @author jbaumann
 */
public class ConfigurationData {
	public static final String CONFIG_FILE = "configFile";
	public static final String PLUGIN_DATA = "pluginData";
	public static final String PLUGIN_HEADER_FILE = "pluginHeaderFile";
	public static final String PLUGIN_PREFIX_PATTERN = "pluginPrefixPattern";
	public static final String SRC_DIRECTORY = "srcDirectory";
	public static final String MODIFIED_STYLE = "modifiedStyle";
	public static final String UNMODIFIED_STYLE = "unmodifiedStyle";
	public static final String PLUGIN_SUFFIX = "suffix";
	public static final String MEM_LIMITS = "memLimits";

	/**
	 * The constructor initializes all the config information.
	 *
	 * @param args
	 *            the command line arguments
	 */
	public ConfigurationData(final String[] args) {
		setupDefaultConfiguration();
		parseCmdline();
		readConfigFile();
	}

	/**
	 * Parse the command line data.
	 */
	private void parseCmdline() {
		// Later

	}

	/**
	 * The default configuration contains the default options.
	 */
	private Map<String, Object> defaultConfig;

	private final String defaultConfigString =
			"# This is the YAML config file for The ESPEasy Config Wizard\n"
					+ "configFile: epwconfig.yaml\n"
					+ "# All Files are searched relative to srcDirectory\n"
					+ "srcDirectory: \".\"\n" + "pluginData: Plugin_sizes.txt\n"
					+ "pluginHeaderFile: enabled_plugins.h\n"
					+ "# pluginPrefixPattern: \"_P|_N\"\n"
					+ "pluginPrefixPattern: \"_P\"\n"
					+ "modifiedStyle: \"-fx-background-color: mistyrose\"\n"
					+ "unmodifiedStyle: \"\"\n" + "suffix: .ino\n"
					+ "memLimits:\n" + "  - name:      \"ESP-8266: 1 MB\"\n"
					+ "    cacheIRam: 9999\n" + "    initRam:   9999\n"
					+ "    roRam:     9999\n" + "    uninitRam: 1000\n"
					+ "    flashRom:  500000\n"
					+ "  - name:      \"ESP-8266: 4 MB\"\n"
					+ "    cacheIRam: 2\n" + "    initRam:   2\n"
					+ "    roRam:     3\n" + "    uninitRam: 4\n"
					+ "    flashRom:  5\n"
					+ "  - name:      \"ESP-8285: 1 MB\"\n"
					+ "    cacheIRam: 3\n" + "    initRam:   2\n"
					+ "    roRam:     3\n" + "    uninitRam: 4\n"
					+ "    flashRom:  5\n";

	/**
	 * Setup the default configuration.
	 */
	private void setupDefaultConfiguration() {
		final Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		final Map<String, Object> config =
				(Map<String, Object>) yaml.load(defaultConfigString);
		defaultConfig = Collections.unmodifiableMap(config);
	}

	/**
	 * The fileConfig contains a configuration read from the config file. If no
	 * config file exists or if it cannot be read, the empty map will be used
	 * instead.
	 */
	private Map<String, Object> fileConfig = new HashMap<>();

	/**
	 * This method tries to read a YAML file and stores it in the static
	 * variable fileConfig if successful. Error handling is only rudimentary.
	 */
	public void readConfigFile() {
		final Yaml yaml = new Yaml();
		Reader input;
		try {
			input = new FileReader(new File(getConfig(CONFIG_FILE).toString()));
			@SuppressWarnings("unchecked")
			final Map<String, Object> data =
					(Map<String, Object>) yaml.load(input);
			fileConfig = data;
		} catch (final FileNotFoundException e) {
			// no real problem, there is no config file
			System.out.println("Info: No Config File");
		}
	}

	/**
	 * This method returns a config value associated with the key. It first
	 * checks the file configuration and if no value is set then the default
	 * configuration value is returned. If no value for a key exists, this
	 * method returns null.
	 *
	 * @param key
	 *            the key for the config value
	 * @return the config value associated to the config key or null, if not
	 *         existent
	 */
	public Object getConfig(final String key) {
		// the values in the file config can have an arbitrary type
		Object option = fileConfig.get(key);

		if (option == null) {
			option = defaultConfig.get(key);
		}

		return option;
	}

}
