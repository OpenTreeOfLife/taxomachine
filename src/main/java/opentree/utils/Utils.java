package opentree.utils;

import java.util.HashSet;

public final class Utils {
    
    public Utils() {}
    
    /**
     * A function for converting string arrays to HashSets of strings, provided as a convenience for preparing string arrays for TNRS.
     * @param input strings
     * @return a set of Strings containing the input
     */
    public static HashSet<String> stringArrayToHashset(String[] strings) {
        HashSet<String> stringSet = new HashSet<String>();
        for (int i = 0; i < strings.length; i++) {
            stringSet.add(strings[i]);
        }
        return stringSet;
    }
}
