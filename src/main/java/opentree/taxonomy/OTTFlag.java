package opentree.taxonomy;

public enum OTTFlag {

	// not a valid taxon. never made available for mapping
	NOT_OTU ("not_otu", false),
	
	// dubious flags: guilty until proven innocent
	BARREN ("barren", false), // no species in this subtree
	ENVIRONMENTAL ("unclassified3", false),
	HYBRID ("hybrid", false),
	UNCLASSIFIED_INHERITED ("unclassified1", false),
	UNCLASSIFIED_EXACT ("unclassified2", false),
	UNCLASSIFIED4 ("unclassified4", false), // still not sure what to call this
	VIRAL ("viral", false),

	// other
	INCERTAE_SEDIS ("incertae_sedis", true), // things explicitly identified as incertae sedis
	INFRASPECIFIC ("infraspecific", true), // obvious meaning

	// TODO: not quite sure what to do with these...
	SUB_SIBLING ("countme", true),
	SUPER_SIBLING ("unplaced", true),

	// user override
	FORCE_VISIBLE ("", true);
	
	String label;
	boolean includeInPrefIndexes;
	
	OTTFlag(String label, boolean includeInPrefIndexes) {
		this.label = label;
		this.includeInPrefIndexes = includeInPrefIndexes;
	}
}
