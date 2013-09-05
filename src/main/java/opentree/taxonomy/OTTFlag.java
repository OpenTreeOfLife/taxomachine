package opentree.taxonomy;

public enum OTTFlag {

	// ==== flags designating suppression unless overriden
	
	/**
	 * not a valid taxon. never made available for mapping
	 */
	NOT_OTU ("not_otu", false),
	
	// dubious flags: guilty until proven innocent
	
	/**
	 * higher taxa with zero children...
	 */
	BARREN ("barren", false),
	
	/**
	 * things with the string "environmental" in the name
	 */
	ENVIRONMENTAL ("environmental", false),
	
	/**
	 * taxa of known hybrid origin
	 */
	HYBRID ("hybrid", false),

	/**
	 * low-rank children of high-rank taxa (e.g. genus child of class)
	 */
	MAJOR_RANK_CONFLICT ("major_rank_conflict", false),

	/**
	 * children of unclassified containers
	 */
	UNCLASSIFIED_INHERITED ("unclassified_inherited", false),
	
	/**
	 * ?
	 */
	UNCLASSIFIED_DIRECT ("unclassified_direct", false),
	
	/**
	 * viruses
	 */
	VIRAL ("viral", false),

	// ===== flags not designating suppression
	
	/**
	 * ?
	 */
	INCERTAE_SEDIS ("incertae_sedis", true),
	
	/**
	 * ?
	 */
	INCERTAE_SEDIS_DIRECT ("incertae_sedis_direct", true),

	/**
	 * contained with a species-rank taxon
	 */
	INFRASPECIFIC ("infraspecific", true),

	/**
	 * a sibling is of a lower (higher?) rank than this taxon
	 */
	SIBLING_LOWER ("sibling_lower", true),
	
	/**
	 * a sibling is of a higher (lower?) rank than this taxon
	 */
	SIBLING_HIGHER ("sibling_higher", true),

	/**
	 * designates that the taxon should not be suppressed even if it has flags designating suppression
	 */
	FORCE_VISIBLE ("forced_visible", true);
	
	String label;
	boolean includeInPrefIndexes;
	
	OTTFlag(String label, boolean includeInPrefIndexes) {
		this.label = label;
		this.includeInPrefIndexes = includeInPrefIndexes;
	}
}
