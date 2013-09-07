package opentree.taxonomy;

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
	MAJOR_RANK_CONFLICT ("major_rank_conflict", false),

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
	 * Taxa with a sibling which is of a lower (higher?) rank than the taxon.
	 */
	SIBLING_LOWER ("sibling_lower", true),
	
	/**
	 * Taxa with a sibling which is of a higher (lower?) rank than the taxon.
	 */
	SIBLING_HIGHER ("sibling_higher", true),

	/**
	 * Designates that the taxon should not be suppressed even if it has flags designating suppression.
	 */
	FORCE_VISIBLE ("forced_visible", true);
	
	String label;
	boolean includeInPrefIndexes;
	
	OTTFlag(String label, boolean includeInPrefIndexes) {
		this.label = label;
		this.includeInPrefIndexes = includeInPrefIndexes;
	}
}
