package org.opentree.tnrs;

/**
 * Remove offending characters from query names for better TNRS matching.
TODO:  Make different methods for cleaning input name strings vs. tip labels?
 * @author joseph
 *
 */
public final class TNRSNameScrubber {
	
	public static final String offendingChars = "[\\Q_~`:;/[]{}|<>,!@#$%^&*()?+=`\\\\\\E\\s]+";

	/**
	 * Returns a string containing all the characters that are considered invalid (i.e. the characters that will be removed by scrubBasic).
	 * @return invalid chars
	 */
	public static String getInvalidChars() {
		return offendingChars;
	}
	
	/**
	 * Returns the list of input names with the last word (as delimited by spaces) removed.
	 * @param inNames
	 * @return cleaned names
	 */
	public static String[] truncateRightmostWord(String[] inNames) {
		return truncateRightmostWord(inNames, " ");
	}

	/**
	 * Returns the list of input names with the last word (as delimited by the regex `separatorExpr`) removed.
	 * @param inNames
	 * @return cleaned names
	 */
	public static String[] truncateRightmostWord(String[] inNames, String separatorExpr) {
		String[] scrubbedNames = new String[inNames.length];

		for (int i = 0; i < inNames.length; i++) {
			String[] parts = inNames[i].split(separatorExpr);
			String newName = "";
			for (int j = 0; j < parts.length - 1; j++) {
				newName = newName.concat(parts[j]);
			}
			scrubbedNames[i] = newName;
		}
		return scrubbedNames;
	}
	
	/**
	 * Returns the array of input strings with invalid characters removed.
	 * A list of the characters considered to be invalid can be obtained by calling getInvalidChars().
	 * @param inNames
	 * @return cleaned names
	 */
	public static String[] scrubBasic(String[] inNames) {

		String[] scrubbedNames = new String[inNames.length];
		
		for (int i = 0; i < inNames.length; i++) {
			// trim whitespace in case offending characters occur at beginning or end of name
			scrubbedNames[i] = inNames[i].replaceAll(offendingChars, " ").trim();
		}
		return scrubbedNames;
	}
}
