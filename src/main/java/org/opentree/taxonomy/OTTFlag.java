package org.opentree.taxonomy;

public enum OTTFlag {

	// ==== flags designating suppression unless overriden
	
	/**
	 * Not a valid taxon--never made available for mapping. This is applied to containers such as "Bacteria incertae sedis",
	 * which in no way can ever represent biological lineages.
	 */
	NOT_OTU ("not_otu", false),
	
	// dubious flags: guilty until proven innocent
	
	/**
	 * Higher taxa with zero species-level children.
	 */
	BARREN ("barren", false),
	
	/**
	 * Things with the string "environmental" in their name.
	 */
	ENVIRONMENTAL ("environmental", false),
	
	/**
	 * Taxa of known hybrid origin.
	 */
	HYBRID ("hybrid", false),

	/**
	 * Low-rank children of high-rank taxa (e.g. genus child of class).
	 */
	MAJOR_RANK_CONFLICT_DIRECT ("major_rank_conflict_direct", false),

	/**
	 * Low-rank children of high-rank taxa (e.g. genus child of class).
	 */
	MAJOR_RANK_CONFLICT_INHERITED ("major_rank_conflict_inherited", false),

	/**
	 * Children of unclassified containers.
	 */
	UNCLASSIFIED_INHERITED ("unclassified_inherited", false),
	
	/**
	 * Taxa with the string "unclassified" in their name, but which have no children and thus are (hopefully) not a container.
	 */
	UNCLASSIFIED_DIRECT ("unclassified_direct", false),
	
	/**
	 * Viruses.
	 */
	VIRAL ("viral", false),
	
	// TODO: add flag for force suppressed
	
	// ===== flags not designating suppression
	
	/**
	 * Children of incertae sedis containers.
	 */
	INCERTAE_SEDIS ("incertae_sedis", true),
	
	/**
	 * Taxa with the string "incertae sedis" in their name, but which have no children and thus are (hopefully) not a container.
	 */
	INCERTAE_SEDIS_DIRECT ("incertae_sedis_direct", true),

	/**
	 * Taxa contained within a species-rank taxon.
	 */
	INFRASPECIFIC ("infraspecific", true),

	/**
	 * Taxon that has a sibling that is of a lower rank than the taxon.
	 */
	SIBLING_LOWER ("sibling_lower", true),
	
	/**
	 * Taxon that has a sibling that is of a higher rank than the taxon.
	 */
	SIBLING_HIGHER ("sibling_higher", true),

	/**
	 * Taxon that for which some children in origin taxonomy were
	 * placed in a different place in the merged taxonomy.
	 * 
	 * From Jonathan Rees: Sort of like "all children are incertae sedis". It means it's a GBIF-only taxon some of whose children are classified by
	 * NCBI as belonging to a disjoint taxon, so the residual children in OTT form only a partial set relative to the taxon concept (whatever it may
	 * have been).  It's a work in progress; I'm thinking about other ways of handling these taxa. (E.g. Mark had suggested putting the remaining
	 * children in the taxon that's the MRCA of all the NCBI children.)
	 */
	TATTERED ("tattered", true),

	/**
	 * Designates that the taxon should not be suppressed even if it has flags designating suppression.
	 */
	FORCE_VISIBLE ("forced_visible", true);
	
	public final String label;
	public final boolean includeInPrefIndexes;
	
	OTTFlag(String label, boolean includeInPrefIndexes) {
		this.label = label;
		this.includeInPrefIndexes = includeInPrefIndexes;
	}
}
