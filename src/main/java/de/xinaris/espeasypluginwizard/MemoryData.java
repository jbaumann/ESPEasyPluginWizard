package de.xinaris.espeasypluginwizard;

/**
 * Encapsulates memory data.
 *
 * @author jbaumann
 */
public class MemoryData {

	private String name;

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the cacheIRam
	 */
	public int getCacheIRam() {
		return cacheIRam;
	}

	/**
	 * @return the initRam
	 */
	public int getInitRam() {
		return initRam;
	}

	/**
	 * @return the roRam
	 */
	public int getRoRam() {
		return roRam;
	}

	/**
	 * @return the uninitRam
	 */
	public int getUninitRam() {
		return uninitRam;
	}

	/**
	 * @return the flashRom
	 */
	public int getFlashRom() {
		return flashRom;
	}

	private final int cacheIRam;
	private final int initRam;
	private final int roRam;
	private final int uninitRam;
	private final int flashRom;

	/**
	 * Public constructor, the values are immutable after the creation.
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
	 */
	public MemoryData(final String name, final int cacheIRam, final int initRam,
			final int roRam, final int uninitRam, final int flashRom) {
		this.name = name;
		this.cacheIRam = cacheIRam;
		this.initRam = initRam;
		this.roRam = roRam;
		this.uninitRam = uninitRam;
		this.flashRom = flashRom;
	}

	@Override
	public String toString() {
		return name + ": cacheIRam=" + cacheIRam + ", initRam=" + initRam
				+ ", roRam=" + roRam + ", uninitRam=" + uninitRam
				+ ", flashRom=" + flashRom;
	}

	/**
	 * format the data in a single string without the name.
	 *
	 * @return a string representation without the name
	 */
	@SuppressWarnings("checkstyle:linelength")
	public String formatContents() {
		final String format =
				"cacheIRam=%1$04d  \tinitRam=%2$04d  \tr/o-Ram=%3$04d  \tuninitRam=%4$04d  \tflashRom=%5$06d";
		return String.format(format, cacheIRam, initRam, roRam, uninitRam,
				flashRom);
	}

}
