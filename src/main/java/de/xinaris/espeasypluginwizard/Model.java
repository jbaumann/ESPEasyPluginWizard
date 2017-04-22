package de.xinaris.espeasypluginwizard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * @author jbaumann
 */
public class Model {
	/**
	 * The pluginDataMap contains all entries in the plugin data file. The key
	 * is the plugin name.
	 */
	@SuppressWarnings("constantname")
	private final ObservableList<PluginData> pluginDataList =
			FXCollections.observableArrayList();

	private final ObservableList<MemoryData> memLimits =
			FXCollections.observableArrayList();

	private String fileName;
	private String srcDir;
	private final String suffix;
	private final int prefixLength = 5; // "_P040"

	/**
	 * @return the srcDir
	 */
	public String getSrcDir() {
		return srcDir;
	}

	/**
	 * @param srcDir
	 *            the srcDir to set
	 */
	public void setSrcDir(final String srcDir) {
		this.srcDir = srcDir;
		analyzeSrcDir();
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Constructor for the model.
	 *
	 * @param pluginDataName
	 *            the name of the file containing the plugin data
	 * @param prefixPattern
	 *            the prefix pattern identifying the relevant plugin lines
	 * @param suffix
	 *            The file suffix used to determine whether a file is a plugin
	 * @throws IOException
	 *             if the file cannot be found or read
	 */
	public Model(final String pluginDataName, final String prefixPattern,
			final String suffix) throws IOException {
		this.suffix = suffix;

		final Path path = Paths.get(pluginDataName);
		if (!Files.exists(path)) {
			System.out.println(
					"Plugin Data File does not exist: " + pluginDataName);
			throw new IOException();
		}

		try {
			Files.lines(path).forEach(line -> parseLine(line, prefixPattern));
		} catch (final IOException e) {
			System.out.println(
					"Couldn't read Plugin Data File: " + pluginDataName);
			throw e;
		}

	}

	/**
	 * Check whether the current mem requirements exceed the reference mem
	 * values.
	 *
	 * @param currentMem
	 *            the current memory requirements
	 * @param reference
	 *            the reference
	 * @return true if the memory requirements are exceeded
	 */
	public boolean exceedsMemReference(final MemoryData currentMem,
			final MemoryData reference) {
		if (currentMem.getCacheIRam() > reference.getCacheIRam()) {
			return true;
		}
		if (currentMem.getInitRam() > reference.getInitRam()) {
			return true;
		}
		if (currentMem.getRoRam() > reference.getRoRam()) {
			return true;
		}
		if (currentMem.getUninitRam() > reference.getUninitRam()) {
			return true;
		}
		if (currentMem.getFlashRom() > reference.getFlashRom()) {
			return true;
		}
		return false;
	}

	/**
	 * Getter for the Memory Limits.
	 *
	 * @return an observable list of memory data objects
	 */
	public ObservableList<MemoryData> getMemLimits() {
		return memLimits;
	}

	/**
	 * Getter for the Plugin Data Objects.
	 *
	 * @return an observable list of plugin data objects
	 */
	public ObservableList<PluginData> getPluginData() {
		return pluginDataList;
	}

	/**
	 * Initialize the memory limits.
	 *
	 * @param limits
	 *            a list of Maps containing the limit information
	 */
	public void initMemLimits(final List<Map<String, Object>> limits) {

		for (final Map<String, Object> limit : limits) {
			// @TODO Check type and null values
			final Object name = limit.get("name");
			final Object cacheIRam = limit.get("cacheIRam");
			final Object initRam = limit.get("initRam");
			final Object roRam = limit.get("roRam");
			final Object uninitRam = limit.get("uninitRam");
			final Object flashRom = limit.get("flashRom");

			if (name == null || cacheIRam == null || initRam == null
					|| roRam == null || uninitRam == null || flashRom == null) {
				System.out.println(
						"Memlimit definition '" + name + "': value missing");

			} else if (name instanceof String && cacheIRam instanceof Integer
					&& initRam instanceof Integer && roRam instanceof Integer
					&& uninitRam instanceof Integer
					&& flashRom instanceof Integer) {
				final MemoryData md = new MemoryData((String) name,
						(Integer) cacheIRam, (Integer) initRam, (Integer) roRam,
						(Integer) uninitRam, (Integer) flashRom);
				memLimits.add(md);
			} else {
				System.out.println("Memlimit definition '" + name
						+ "': a value has a wrong type");
			}
		}
	}

	/**
	 * Parse each line of the plugin data file and add its information to the
	 * internal structures. If a line is not parseable it is ignored.
	 *
	 * @param line
	 *            contains the current line from the plugin data file
	 * @param prefixPattern
	 *            the prefix pattern describes the part that allows to recognize
	 *            a relevant line
	 */
	private void parseLine(final String line, final String prefixPattern) {
		if (!line.startsWith("src/")) {
			return;
		}
		final String[] fields = line.split("\\|");
		final int srcOffset = 4;
		fields[0] = fields[0].substring(srcOffset, fields[0].length());
		for (int i = 0; i < fields.length; i++) {
			fields[i] = fields[i].trim();
		}

		// determine whether filename is incomplete
		boolean incomplete = true;
		if (fields[0].endsWith(".ino")) {
			incomplete = false;
		}

		// determine whether readonly
		final PluginData pd = new PluginData(fields,
				nameIsReadOnly(fields[0], prefixPattern), incomplete);
		pd.calcMacroName(suffix);
		pluginDataList.add(pd);
	}

	/**
	 * Check whether a plugin name is a plugin that can be enabled/disabled or
	 * if it is a system plugin/file.
	 *
	 * @param name
	 *            the file name to check
	 * @param prefixPattern
	 *            the current prefix pattern
	 * @return true if the plugin name denotes a read-only plugin
	 */
	protected boolean nameIsReadOnly(final String name,
			final String prefixPattern) {

		return !(name.matches("^(" + prefixPattern + ").*"));
	}

	/**
	 * Save the current model into the given file.
	 *
	 * @param saveFile
	 *            the File object denoting the file in which to save the data
	 * @return true if saving the data was successful
	 */
	public boolean save(final File saveFile) {
		try (BufferedWriter writer =
				Files.newBufferedWriter(saveFile.toPath())) {

			// sort the plugins by name
			pluginDataList
					.sort((pl1, pl2) -> pl1.getName().compareTo(pl2.getName()));
			for (final PluginData p : pluginDataList) {
				final String name = p.getMacroName();
				if (!p.isEnabled()) {
					writer.write("// ");
				}
				writer.write("#define " + name + System.lineSeparator());

			}
			writer.flush();
			// be conservative, only set to unmodified after everything is
			// written
			for (final PluginData pl : pluginDataList) {
				pl.setModified(false);
			}

			return true;

		} catch (final IOException e1) {
			System.out.println(
					"Something went wrong writing file " + saveFile.getPath());
		}
		return false;
	}

	/**
	 * Load the given file, adjust the plugin status and add new plugins if they
	 * are not found in the plugin data list.
	 *
	 * @param loadFile
	 *            the File object denoting the file to load
	 * @param prefixPattern
	 *            the pattern that denotes the prefix of plugins that can be
	 *            enabled
	 * @return true if the load was successful
	 */
	public boolean load(final File loadFile, final String prefixPattern) {

		final HashMap<String, PluginData> map = new HashMap<>();
		for (final PluginData pl : pluginDataList) {
			final String shortenedMacro =
					pl.getMacroName().substring(0, prefixLength - 1); // no
																		// underscore
			map.put(shortenedMacro, pl);
		}
		try (Stream<String> stream = Files.lines(loadFile.toPath())) {
			stream.forEach(line -> {
				final Matcher m = Pattern.compile("(//)? *#define +(\\S+).*")
						.matcher(line);
				if (m.matches()) {
					final boolean enabled = m.group(1) == null;
					final String macroName = m.group(2);
					final String shortenedMacro =
							macroName.substring(0, prefixLength - 1); // no
																		// underscore
					final PluginData pl = map.get(shortenedMacro);
					if (pl != null) {
						pl.setEnabled(enabled);
						pl.setModified(false);
						if (!pl.getMacroName().equals(macroName)) {
							System.out.println(
									"Warning: : Prefix " + shortenedMacro
											+ ": actual values differ");
							System.out.println("Keeping " + pl.getMacroName()
									+ " instead of " + macroName);
						}
					} else {
						// Create new Plugin Data
						final String name = "_" + macroName;
						final boolean readOnly =
								nameIsReadOnly(name, prefixPattern);
						final PluginData newData = new PluginData(name, 0, 0, 0,
								0, 0, readOnly, false);
						newData.setEnabled(enabled);
						newData.setModified(false);
						newData.calcMacroName(suffix);
						pluginDataList.add(newData);
						System.out.println("Found new entry: " + name);
					}
				} else {
					System.out.println(
							"Line did not match macro definition: " + line);
				}
			});
		} catch (final IOException e) {
			System.out.println("Something went wrong loading the file");
			return false;
		}
		return true;
	}

	/**
	 * This method analyzes the source directory and adds incomplete information
	 * (e.g. filenames) and additional plugin names.
	 */
	private void analyzeSrcDir() {
		final File dir = new File(srcDir);

		final File[] files = dir.listFiles((FileFilter) file -> {
			final String name = file.getName();
			return file.isFile() && name.endsWith(suffix)
					&& name.startsWith("_") && !name.startsWith("__");
		});
		for (final File file : files) {
			// we could use a non-trivial implementation sorting the files
			// first, but with the low number of entries it simply is not worth
			// it.
			checkAndAddFile(file);
		}
	}

	/**
	 * Check whether the given file name is already in the list of plugins
	 * (maybe incomplete). If not found, add it, if incomplete, complete it.
	 *
	 * @param file
	 *            the plugin filename
	 */
	private void checkAndAddFile(final File file) {
		final String name = file.getName();
		for (final PluginData pl : pluginDataList) {
			final String pluginName = pl.getName();
			if (pluginName.equals(name)) {
				return;
			}
			final String uniquePrefix = pluginName.substring(0, prefixLength);
			if (name.startsWith(uniquePrefix) && pl.hasIncompleteFileName()) {
				pl.setName(name, suffix);
				pl.setIncompleteFileName(false);
				if (!name.startsWith(pluginName)) {
					System.out.println("Warning: Prefix " + uniquePrefix
							+ ": actual values differ");
					System.out.println("Setting to " + name);
				}
				return;
			}
		}
		// ok, the name is not in the list. We create a new pluginData entry and
		// add it.
		final PluginData newData =
				new PluginData(name, 0, 0, 0, 0, 0, false, false);
		newData.calcMacroName(suffix);
		pluginDataList.add(newData);
		System.out.println("Found new entry: " + name);
	}

}
