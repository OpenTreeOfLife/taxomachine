package opentree;

/**
 * Defines relationship indexes for the db. Not clear that these are even being used...
 * @author cody
 *
 */
public enum RelIndexDescription {
    SOURCE_TYPE ("taxSources");

    public final String namePrefix;

    RelIndexDescription(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}