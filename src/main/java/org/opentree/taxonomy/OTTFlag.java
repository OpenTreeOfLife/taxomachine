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
	 * Descendants of ENVIRONMENTAL taxa.
     * New 2014-04-26
	 */
	ENVIRONMENTAL_INHERITED ("environmental_inherited", false),

	/**
	 * Extinct taxa; replaced extinct_direct
	 */
	EXTINCT ("extinct", true),

	/**
	 * Extinct taxa. Replaced by 'extinct'
	 */
	 @Deprecated
	EXTINCT_DIRECT ("extinct_direct", true), // TODO: should these be hidden?

	/**
	 * Extinct taxa?
	 */
	EXTINCT_INHERITED ("extinct_inherited", true), // TODO: should these be hidden?

	/**
	 * Low-rank children of high-rank taxa (e.g. genus child of class).
	 * Replaces major_rank_conflict_direct
	 */
	MAJOR_RANK_CONFLICT ("major_rank_conflict", true),

	/**
	 * Low-rank children of high-rank taxa (e.g. genus child of class).
	 * Replaced by 'major_rank_conflict'
	 */
	 @Deprecated
	MAJOR_RANK_CONFLICT_DIRECT ("major_rank_conflict_direct", true),

	/**
	 * Descendants of low-rank children of high-rank taxa (e.g. genus child of class).
	 */
	MAJOR_RANK_CONFLICT_INHERITED ("major_rank_conflict_inherited", true),

	/**
	 * Taxa that are or were children of "unclassified" containers
     * (pseudo-taxa in NCBI with the string "unclassified" in their name).
     * New 2014-04-26
	 */
	UNCLASSIFIED ("unclassified", true),

	/**
	 * Taxa with the string "unclassified" in their name, but which
	 * have no children and thus are (hopefully) no longer a
	 * container.
     * 2014-04-26 DEPRECATED, these are now just labeled "not_otu".
	 */
	UNCLASSIFIED_DIRECT ("unclassified_direct", false),

	/**
	 * Descendants of UNCLASSIFIED taxa.
	 */
	UNCLASSIFIED_INHERITED ("unclassified_inherited", true),

	/**
	 * Viruses.
	 */
	VIRAL ("viral", false),

	/**
	 * Taxa that have been intentionally hidden (by a curator? for some reason other than those listed in other flags?).
	 */
	HIDDEN ("hidden", false),

	/**
	 * Taxa contained within taxa have been intentionally hidden (by a curator? for some reason other than those listed in other flags?).
	 */
	HIDDEN_INHERITED ("hidden_inherited", false),

	// ===== flags not designating suppression

	/**
	 * Taxa that have been manually edited.
	 */
	EDITED ("edited", true),

	/**
	 * Taxa of known hybrid origin.
	 */
	HYBRID ("hybrid", true),

	// TODO: these incertae sedis designations are confusing...

	/**
	 * Children of incertae sedis containers, and others flagged as incertae sedis in their parent. Replaced incertae_sedis_direct
	 */
	INCERTAE_SEDIS ("incertae_sedis", true),

	/**
	 * Descendants of INCERTAE_SEDIS taxa.
	 */
	INCERTAE_SEDIS_INHERITED ("incertae_sedis_inherited", true), // TODO: is this deprecated/superseded by the "incertae_sedis" flag?

	/**
	 * Taxa with the string "incertae sedis" in their name, but which have no children and thus are (hopefully) not a container.
     * 2014-04-26 DEPRECATED, these are now only flagged "not_otu".
	 */
	@Deprecated
	INCERTAE_SEDIS_DIRECT ("incertae_sedis_direct", true),

	/**
	 * Taxa contained within a species-rank taxon.
	 */
	INFRASPECIFIC ("infraspecific", true),

	/**
	 * Taxon that has a sibling that is of a lower rank than the taxon.
	 */
	 @Deprecated
	SIBLING_LOWER ("sibling_lower", true),

	/**
	 * Taxon that has a sibling that is of a higher rank than the taxon.
	 */
	SIBLING_HIGHER ("sibling_higher", true),

	/**
	 * Taxon for which some children in origin taxonomy were
	 * placed in a different place in the merged taxonomy;
     * casualties from a conflict between two taxonomies.
	 *
	 * From Jonathan Rees: Sort of like "all children are incertae sedis". It means it's a GBIF-only taxon some of whose children are classified by
	 * NCBI as belonging to a disjoint taxon, so the residual children in OTT form only a partial set relative to the taxon concept (whatever it may
	 * have been).  It's a work in progress; I'm thinking about other ways of handling these taxa. (E.g. Mark had suggested putting the remaining
	 * children in the taxon that's the MRCA of all the NCBI children.)
	 */
	TATTERED ("tattered", true),

	/**
	 * Taxa that are descendants of a TATTERED taxon.
	 */
	TATTERED_INHERITED("tattered_inherited", true),

	/**
	 * Designates that the taxon should not be suppressed even if it has flags designating suppression.
	 */
	FORCE_VISIBLE ("forced_visible", true),

	/**
	* Similar to incertae_sedis (this means a child of an inconsistent taxon, where t is inconsistent if it occurs in a lower-priority taxonomy but is inconsistent with the higher-priority taxonomies. 'tattered' is now deprecated).
	*/
	UNPLACED ("unplaced", true),

  /**
  * Descends from a placed taxon.
  */
  UNPLACED_INHERITED ("unplaced_inherited", true),

	/**
	* (formerly 'tattered') Taxon in lower priority taxonomy that is inconsistent.
	*/
	INCONSISTENT ("inconsistent", true),

	/**
	This taxon was consistent with another and got folded into it.
	Taxon is hidden, children aren't. Taxon may be revived if it's learned later
	that the it is actually different.
	*/
	 MERGED ("merged", true),

	 /**
	 Treat same as incertae_sedis, merged, and inconsistent - the 'taxon' was formerly a 'bucket' but is now empty and is preserved as a placeholder.
	*/
	WAS_CONTAINER ("was_container", false);

	public final String label;
	public final boolean includeInPrefIndexes;

	OTTFlag(String label, boolean includeInPrefIndexes) {
		this.label = label;
		this.includeInPrefIndexes = includeInPrefIndexes;
	}
}
