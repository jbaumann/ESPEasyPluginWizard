package de.xinaris.espeasypluginwizard;

/**
 * Encapsulates the plugin details.
 *
 * @author jbaumann
 */
public class PluginData extends MemoryData {

	private final boolean readOnly;
	private boolean enabled = false;
	private boolean modified = false;
	private boolean incompleteFileName;

	/**
	 * @param incompleteFileName
	 *            the incompleteFileName to set
	 */
	public void setIncompleteFileName(final boolean incompleteFileName) {
		this.incompleteFileName = incompleteFileName;
	}

	private String macroName;

	/**
	 * Set the name and calculate the macro name.
	 *
	 * @param name
	 *            the name to set
	 * @param suffix
	 *            the suffix to be stripped
	 */
	public void setName(final String name, final String suffix) {
		super.setName(name);
		calcMacroName(suffix);
	}

	/**
	 * @return the incompleteFileName
	 */
	public boolean hasIncompleteFileName() {
		return incompleteFileName;
	}

	/**
	 * @param changed
	 *            the changed to set
	 */
	public void setModified(final boolean changed) {
		this.modified = changed;
	}

	/**
	 * @return the changed
	 */
	public boolean isModified() {
		return modified;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(final boolean enabled) {
		if (this.enabled != enabled) {
			modified = !modified; // toggle changed flag
		}
		this.enabled = enabled;
	}

	/**
	 * String field constructor.
	 *
	 * @param fields
	 *            the single entries of the plugin data file as strings
	 * @param readonly
	 *            true if this entry cannot be disabled
	 * @param incompletename
	 *            marks whether the name has had the needed suffix
	 */
	@SuppressWarnings("magicnumber")
	public PluginData(final String[] fields, final boolean readonly,
			final boolean incompletename) {
		this(fields[0], Integer.parseInt(fields[1]),
				Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
				Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
				readonly, incompletename);
	}

	/**
	 * This is the normal constructor.
	 *
	 * @param name
	 *            contains the name of this entry
	 * @param cacheIRam
	 *            the cacheIRAM size
	 * @param initRam
	 *            the initialized RAM size
	 * @param roRam
	 *            the r/o RAM size
	 * @param uninitRam
	 *            the uninitialized RAM size
	 * @param flashRom
	 *            the flash ROM size
	 * @param incompleteName
	 *            marks whether the name has had the needed suffix
	 * @param readonly
	 *            decides whether this entry can be disabled
	 */
	@SuppressWarnings("checkstyle:parameternumber")
	public PluginData(final String name, final int cacheIRam, final int initRam,
			final int roRam, final int uninitRam, final int flashRom,
			final boolean readonly, final boolean incompleteName) {
		super(name, cacheIRam, initRam, roRam, uninitRam, flashRom);
		this.readOnly = readonly;
		this.incompleteFileName = incompleteName;
		if (readonly) {
			this.enabled = true;
		}

	}

	@Override
	public String toString() {
		return "PluginData [readOnly=" + readOnly + ", enabled=" + enabled
				+ ", modified=" + modified + ", incompleteName="
				+ incompleteFileName + "], " + super.toString();
	}

	/**
	 * Return the macro name for this plugn.
	 *
	 * @return the macro name
	 */
	public String getMacroName() {
		return macroName;
	}

	/**
	 * Calculate the macro name from plugin name and removes suffix. Removes the
	 * leading underscore.
	 *
	 * @param suffix
	 *            the suffix to be stripped
	 */
	public void calcMacroName(final String suffix) {

		final String extension = suffix;
		String name = getName();
		if (name.startsWith("_")) {
			name = name.substring(1); // remove underscore
		}
		if (name.endsWith(extension)) {
			name = name.substring(0, name.length() - extension.length());
		}
		macroName = name;
	}

}
