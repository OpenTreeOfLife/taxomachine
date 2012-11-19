package opentree.tnrs;

/*
 Remove offending characters from query names for better TNRS matching.

There is currently a bunch of extra debugging stuff in here that will be purged when complete.

Use 'review' to print out original and cleaned names.

TODO:  Do we want to apply different cleaning to input name strings vs. tip labels?
 */

public final class TNRSNameScrubber {
	
//	private static String[]	dirtyNames;
//	private static String[]	cleanedNames;
    public static final String offendingChars = "[\\Q_~`:;/[]{}|<>,!@#$%^&*()?+=`\\\\\\E]+";

    
	public TNRSNameScrubber(/*String[] inNames */) {
//		dirtyNames = inNames;
//		cleanedNames = scrubNames(dirtyNames);
	}
	
	public static String[] scrubNames(String[] dirtyNames) {

	    String[] cleanedNames = new String[dirtyNames.length];
/*
special characters needing escaping: .|*?+(){}[]^$\
	\\.\\|\\*\\?\\+\\(\\)\\{\\}\\[\\]\\^\\$\\\
tip labels will not involve the following (unless in quotes) as these have special meaning in trees:
	:,()[];
these should not survive tree import.
the following will be the most frequent:
	_.
 */
//		cleanedNames = (String[]) dirtyNames.clone(); // copying is only for debugging purposes
		
// we may want to alter this
//		String offendingChars = "[_\\s\\._~`:;/\\[\\]\\{\\}\\|<>,!@#\\$%\\^&*\\(\\)\\?\\+=`\\\\-]+";
		
		for (int i = 0; i < cleanedNames.length; i++) {
			cleanedNames[i] = dirtyNames[i].replaceAll(offendingChars, " ").trim(); // trim is used in case offending characters occur at beginning or end of name
		}
		return cleanedNames;
	}
	
// for debugging:
/*	public void review () {
		int maxLength = 0;
		for (int i = 0; i < dirtyNames.length; i++) {
			if (dirtyNames[i].length() > maxLength)
				maxLength = dirtyNames[i].length();
		}
		
		// print original and cleaned labels
		for (int i = 0; i < cleanedNames.length; i++) {
			System.out.print(dirtyNames[i] + "   ");
			int diff = maxLength - dirtyNames[i].length();
			while (diff > 0) {
				System.out.print(" ");
				diff--;
			}
			System.out.print(cleanedNames[i] + "\n");
		}
	} */
	
/*	public String[] cleanedNames() {
		return cleanedNames;
	} */
}